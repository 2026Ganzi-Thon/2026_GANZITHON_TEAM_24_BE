package com.ganzithon.toro.domain.vent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.stt.client.GroqSttClient;
import com.ganzithon.toro.domain.stt.controller.SttController;
import com.ganzithon.toro.domain.stt.dto.Transcription;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.domain.vent.controller.VentSessionController;
import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionResponse;
import com.ganzithon.toro.domain.vent.dto.FlowDtos.*;
import com.ganzithon.toro.domain.vent.service.*;
import com.ganzithon.toro.global.config.SecurityConfig;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import com.ganzithon.toro.global.security.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = {VentSessionController.class, SttController.class}, properties = {
        "jwt.secret=test-secret-test-secret-test-secret-1234", "jwt.expiration=7d",
        "cors.allowed-origins=http://localhost:5173", "vent.session-ttl=15m"})
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class, VentSessionStore.class})
class VentWorkflowControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JwtProvider jwt;
    @Autowired VentSessionStore store;
    @MockitoBean UserRepository users;
    @MockitoBean EmotionSuggestionService emotions;
    @MockitoBean ReflectionAiService ai;
    @MockitoBean GroqSttClient groq;
    private String token;
    private static final String BASE = "/api/v1/vent/sessions";

    @BeforeEach void setup() {
        store.clear(); given(users.existsById(1L)).willReturn(true); token = "Bearer " + jwt.createToken(1L);
    }

    @Test void 음성부터_수정한_최종결과_삭제까지_실제_인증으로_연결한다() throws Exception {
        String id = create(); String path = BASE + "/" + id;
        given(groq.transcribe(any())).willReturn(new Transcription("인식한 하소연", 2));
        mvc.perform(multipart("/api/v1/stt").file(file()).param("sessionId", id).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.text").value("인식한 하소연"));
        given(ai.empathize("수정한 하소연")).willReturn(new Empathy("답답하셨겠어요."));
        ok(post(path + "/messages"), "{\"text\":\"수정한 하소연\"}");
        given(emotions.suggest(anyString())).willReturn(new EmotionSuggestionResponse("공감", List.of("서운함", "답답함"), true));
        ok(post(path + "/emotions"), "{\"text\":\"수정한 하소연\"}");
        ok(post(path + "/emotions/confirm"), "{\"emotion\":\"\"}");
        mvc.perform(get(path + "/calming").header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.canSkip").value(true));
        ok(post(path + "/calming/complete"), "{\"skipped\":true}");
        given(ai.needs(eq(""), any())).willReturn(new NeedsSuggestion(List.of("존중"), "미리 알려주기", true));
        ok(post(path + "/needs/suggestions"), "{\"situation\":\"새로 적은 상황\",\"desiredResponse\":\"\"}");
        ok(put(path + "/reflection"), "{\"situation\":\"새로 적은 상황\",\"emotion\":\"\",\"need\":\"존중\",\"desiredResponse\":\"미리 알려주기\"}");
        given(ai.summarize(any())).willReturn(new SummarySuggestion("AI 정리 초안", true));
        ok(post(path + "/summary/suggestions"), "");
        ok(put(path + "/summary"), "{\"summary\":\"사용자가 고친 3인칭 정리\"}");
        given(ai.plan(anyString(), any())).willReturn(new PlanSuggestion("조건", "행동", true));
        ok(post(path + "/plan/suggestions"), "");
        verify(ai).plan(eq("사용자가 고친 3인칭 정리"), any());
        ok(put(path + "/plan"), "{\"ifSituation\":\"약속이 바뀌면\",\"thenAction\":\"내 일정을 먼저 정리한다\"}");
        mvc.perform(get(path).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stage").value("RESULT"))
                .andExpect(jsonPath("$.data.summary").value("사용자가 고친 3인칭 정리"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("인식한 하소연"))));
        mvc.perform(delete(path).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.serverStateCleared").value(true))
                .andExpect(jsonPath("$.data.requestsSettled").value(true))
                .andExpect(jsonPath("$.data.externalDeletionVerified").value(false));
        mvc.perform(get(path).header("Authorization", token)).andExpect(status().isGone());
    }

    @Test void 감정확인_이후_하소연과_STT는_차단된다() throws Exception {
        String id = create(); String path = BASE + "/" + id;
        ok(post(path + "/emotions/confirm"), "{}");
        mvc.perform(post(path + "/messages").header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"원문 재전송\"}")).andExpect(status().isConflict());
        mvc.perform(multipart("/api/v1/stt").file(file()).param("sessionId", id).header("Authorization", token))
                .andExpect(status().isConflict());
        verifyNoInteractions(ai, groq, emotions);
    }

    @Test void 미로그인과_타인세션_접근은_차단된다() throws Exception {
        String id = create();
        mvc.perform(post(BASE)).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/v1/stt").file(file()).param("sessionId", id)).andExpect(status().isUnauthorized());
        given(users.existsById(2L)).willReturn(true);
        String other = "Bearer " + jwt.createToken(2L);
        mvc.perform(get(BASE + "/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(delete(BASE + "/" + id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(multipart("/api/v1/stt").file(file()).param("sessionId", id).header("Authorization", other))
                .andExpect(status().isNotFound());
        verifyNoInteractions(ai, groq, emotions);
    }

    @Test void 잘못된_입력과_필수파일누락은_외부호출_전에_거절한다() throws Exception {
        String id = create();
        mvc.perform(post(BASE + "/" + id + "/messages").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\" \"}")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/stt").param("sessionId", id).header("Authorization", token)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/v1/stt").file(file()).param("sessionId", "invalid").header("Authorization", token)).andExpect(status().isBadRequest());
        ok(post(BASE + "/" + id + "/emotions/confirm"), "{}");
        mvc.perform(post(BASE + "/" + id + "/calming/complete").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        verifyNoInteractions(ai, groq, emotions);
    }

    @Test void AI장애가_나도_감정미확정으로_진행한다() throws Exception {
        String id = create();
        given(emotions.suggest(anyString())).willThrow(new BusinessException(ErrorCode.AI_NOT_CONFIGURED));
        mvc.perform(post(BASE + "/" + id + "/emotions").header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"서운해요\"}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error.code").value("AI_001"));
        ok(post(BASE + "/" + id + "/emotions/confirm"), "{}");
        assertThat(store.get(1, UUID.fromString(id)).stage()).isEqualTo(Stage.CALMING);
    }

    private String create() throws Exception {
        String json = mvc.perform(post(BASE).header("Authorization", token)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).path("data").path("sessionId").asText();
    }
    private void ok(MockHttpServletRequestBuilder request, String body) throws Exception {
        mvc.perform(request.header("Authorization", token).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"));
    }
    private MockMultipartFile file() { return new MockMultipartFile("file", "recording.webm", "audio/webm", new byte[]{1}); }
}
