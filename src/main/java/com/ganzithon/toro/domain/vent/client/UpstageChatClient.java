package com.ganzithon.toro.domain.vent.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.vent.config.UpstageProperties;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UpstageChatClient {

    private static final int MAX_RESPONSE_BYTES = 65_536;
    private static final Map<String, Object> RESPONSE_FORMAT = Map.of(
            "type", "json_schema",
            "json_schema", Map.of("name", "emotion_suggestions", "strict", true,
                    "schema", Map.of("type", "object", "additionalProperties", false,
                            "required", List.of("empathy", "candidates"),
                            "properties", Map.of(
                                    "empathy", Map.of("type", "string"),
                                    "candidates", Map.of("type", "array", "items", Map.of("type", "string"))))));

    private final RestClient client;
    private final ObjectMapper mapper;
    private final UpstageProperties properties;

    public UpstageChatClient(@Qualifier("upstageRestClient") RestClient client,
                             ObjectMapper mapper, UpstageProperties properties) {
        this.client = client;
        this.mapper = mapper;
        this.properties = properties;
    }

    public String suggest(String systemPrompt, String text) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_NOT_CONFIGURED);
        }
        try {
            return client.post().uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(properties.apiKey().strip()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", properties.model(), "stream", false, "max_tokens", 1024,
                            "messages", List.of(Map.of("role", "system", "content", systemPrompt),
                                    Map.of("role", "user", "content", text)),
                            "response_format", RESPONSE_FORMAT))
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 429) {
                            throw new BusinessException(ErrorCode.AI_BUSY);
                        }
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            // 외부 오류 본문은 읽거나 로그에 출력하지 않는다.
                            throw new BusinessException(ErrorCode.AI_REQUEST_FAILED);
                        }
                        byte[] bytes = response.getBody().readNBytes(MAX_RESPONSE_BYTES + 1);
                        if (bytes.length > MAX_RESPONSE_BYTES) {
                            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
                        }
                        JsonNode root;
                        try {
                            root = mapper.readerFor(JsonNode.class)
                                    .with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(bytes);
                        } catch (Exception ignored) {
                            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
                        }
                        JsonNode choice = root == null ? null : root.path("choices").path(0);
                        if (choice == null || !"stop".equals(choice.path("finish_reason").asText())
                                || !choice.path("message").path("content").isTextual()) {
                            throw new BusinessException(ErrorCode.AI_INVALID_RESPONSE);
                        }
                        return choice.path("message").path("content").textValue();
                    });
        } catch (BusinessException e) {
            throw e;
        } catch (Exception ignored) {
            // 원문, 키, 공급자의 응답이 전역 예외 로그에 포함되지 않게 원인을 연결하지 않는다.
            throw new BusinessException(ErrorCode.AI_REQUEST_FAILED);
        }
    }
}
