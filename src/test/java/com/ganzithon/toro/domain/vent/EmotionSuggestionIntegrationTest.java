package com.ganzithon.toro.domain.vent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.vent.client.UpstageChatClient;
import com.ganzithon.toro.domain.vent.config.UpstageProperties;
import com.ganzithon.toro.domain.vent.dto.EmotionSuggestionRequest;
import com.ganzithon.toro.domain.vent.service.EmotionSuggestionService;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class EmotionSuggestionIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();
    private MockRestServiceServer server;
    private EmotionSuggestionService service;
    private RestClient client;

    @BeforeEach
    void setup() {
        var builder = RestClient.builder().baseUrl("https://api.upstage.ai/v1");
        server = MockRestServiceServer.bindTo(builder).build();
        client = builder.build();
        service = serviceWithKey("test-key");
    }

    private EmotionSuggestionService serviceWithKey(String key) {
        var properties = new UpstageProperties(key, "https://api.upstage.ai/v1", "solar-pro4",
                Duration.ofSeconds(5), Duration.ofSeconds(30));
        return new EmotionSuggestionService(new UpstageChatClient(client, mapper, properties), mapper);
    }

    private String envelope(String content, String finishReason) throws Exception {
        return mapper.writeValueAsString(Map.of("choices", List.of(Map.of(
                "finish_reason", finishReason, "message", Map.of("content", content)))));
    }

    @Test
    void 성공하면_공감과_미확정_후보를_반환한다() throws Exception {
        server.expect(requestTo("https://api.upstage.ai/v1/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andExpect(jsonPath("$.model").value("solar-pro4"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value("내 시간을 존중받지 못한 것 같아요"))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.max_tokens").value(1024))
                .andRespond(withSuccess(envelope(
                        "{\"empathy\":\"서운할 수 있겠어요.\",\"candidates\":[\"서운함\",\"답답함\"]}", "stop"),
                        MediaType.APPLICATION_JSON));
        var result = service.suggest("내 시간을 존중받지 못한 것 같아요");
        assertThat(result.candidates()).containsExactly("서운함", "답답함");
        assertThat(result.canSkip()).isTrue();
        assertThat(result.toString()).doesNotContain("서운");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\"]}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\",\"분노\"]}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\",\"슬픔\",\"불안\",\"외로움\"]}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\",123]}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\",\"<script>\"]}",
            "{\"empathy\":\"공감\",\"candidates\":[\"분노\",\"불안\"]} {}"})
    void 잘못된_모델_응답은_후보로_노출하지_않는다(String content) throws Exception {
        server.expect(anything()).andRespond(withSuccess(envelope(content, "stop"), MediaType.APPLICATION_JSON));
        expectError(service, ErrorCode.AI_INVALID_RESPONSE);
        server.verify();
    }

    @Test
    void 토큰_한도로_잘린_결과는_반환하지_않는다() throws Exception {
        server.expect(anything()).andRespond(withSuccess(envelope("{}", "length"), MediaType.APPLICATION_JSON));
        expectError(service, ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void 외부_응답_크기를_제한한다() {
        server.expect(anything()).andRespond(withSuccess("x".repeat(65_537), MediaType.APPLICATION_JSON));
        expectError(service, ErrorCode.AI_INVALID_RESPONSE);
    }

    @Test
    void 외부_오류_본문과_원인은_예외에_남기지_않는다() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.UNAUTHORIZED).body("private-text test-key"));
        expectError(service, ErrorCode.AI_REQUEST_FAILED);
    }

    @Test
    void 요청_제한은_재시도하지_않는다() {
        server.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        expectError(service, ErrorCode.AI_BUSY);
        server.verify();
    }

    @Test
    void 타임아웃을_안전한_오류로_바꾼다() {
        server.expect(anything()).andRespond(withException(new SocketTimeoutException("private-text")));
        expectError(service, ErrorCode.AI_REQUEST_FAILED);
    }

    @Test
    void 키가_없으면_외부_호출을_하지_않는다() {
        expectError(serviceWithKey(""), ErrorCode.AI_NOT_CONFIGURED);
        server.verify();
    }

    @Test
    void 원문과_키는_toString에서_감춘다() {
        assertThat(new EmotionSuggestionRequest("private-text").toString()).doesNotContain("private-text");
        assertThat(new UpstageProperties("secret-key", "", "", null, null).toString())
                .doesNotContain("secret-key");
    }

    private void expectError(EmotionSuggestionService target, ErrorCode code) {
        assertThatThrownBy(() -> target.suggest("테스트 입력"))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(code);
                    assertThat(e.getCause()).isNull();
                    assertThat(e.getMessage()).doesNotContain("private-text", "test-key");
                });
    }
}
