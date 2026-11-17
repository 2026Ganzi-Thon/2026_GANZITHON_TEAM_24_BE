package com.ganzithon.toro.domain.vent;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.user.repository.UserRepository;
import com.ganzithon.toro.domain.vent.controller.EmotionSuggestionController;
import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionResponse;
import com.ganzithon.toro.domain.vent.service.EmotionSuggestionService;
import com.ganzithon.toro.global.config.SecurityConfig;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import com.ganzithon.toro.global.security.JwtAuthenticationFilter;
import com.ganzithon.toro.global.security.JwtProvider;
import com.ganzithon.toro.global.security.SecurityErrorHandler;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EmotionSuggestionController.class, properties = {
        "jwt.secret=test-secret-test-secret-test-secret-1234", "jwt.expiration=7d",
        "cors.allowed-origins=http://localhost:5173"})
@Import({SecurityConfig.class, JwtProvider.class, JwtAuthenticationFilter.class, SecurityErrorHandler.class})
class EmotionSuggestionControllerTest {

    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired ObjectMapper mapper;
    @MockitoBean EmotionSuggestionService service;
    @MockitoBean UserRepository users;

    private String token() {
        given(users.existsById(1L)).willReturn(true);
        return "Bearer " + jwt.createToken(1L);
    }

    @Test
    void 로그인하지_않으면_외부_AI를_호출하지_않는다() throws Exception {
        mvc.perform(post("/api/v1/vent/emotions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\":\"서운해요\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("COMMON_002"));
        verifyNoInteractions(service);
    }

    @Test
    void 기존_JWT로_인증하고_캐시하지_않는_응답을_준다() throws Exception {
        given(service.suggest(anyString())).willReturn(
                new EmotionSuggestionResponse("서운할 수 있겠어요.", List.of("서운함", "답답함"), true));
        mvc.perform(post("/api/v1/vent/emotions").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"서운해요\"}"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidates.length()").value(2))
                .andExpect(jsonPath("$.data.canSkip").value(true));
    }

    @Test
    void 빈_문자와_긴_문자는_과금_호출_전에_거절한다() throws Exception {
        String token = token();
        for (String text : List.of("   ", "가".repeat(2001))) {
            mvc.perform(post("/api/v1/vent/emotions").header("Authorization", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(Map.of("text", text))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("COMMON_001"));
        }
        verifyNoInteractions(service);
    }

    @Test
    void 키_미설정을_공통_오류_형식으로_반환한다() throws Exception {
        given(service.suggest(anyString())).willThrow(new BusinessException(ErrorCode.AI_NOT_CONFIGURED));
        mvc.perform(post("/api/v1/vent/emotions").header("Authorization", token())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"서운해요\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("AI_001"));
    }
}
