package com.ganzithon.toro.domain.vent;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ganzithon.toro.domain.vent.client.UpstageChatClient;
import com.ganzithon.toro.domain.vent.dto.FlowDtos.*;
import com.ganzithon.toro.domain.vent.service.ReflectionAiService;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class ReflectionAiServiceTest {
    private final UpstageChatClient client = mock(UpstageChatClient.class);
    private final ObjectMapper mapper = new ObjectMapper();
    private final ReflectionAiService ai = new ReflectionAiService(client, mapper);

    @Test void 짧은_공감은_초안만_반환한다() {
        when(client.complete(anyString(), anyString(), anyMap())).thenReturn("{\"empathy\":\"답답하셨겠어요.\"}");
        assertThat(ai.empathize("할 말이 많아요").empathy()).isEqualTo("답답하셨겠어요.");
    }

    @Test void 욕구에는_확정감정과_새_입력만_보낸다() throws Exception {
        when(client.complete(anyString(), anyString(), anyMap())).thenReturn("{\"needs\":[\"존중\",\"예측 가능성\"],\"desiredResponse\":\"미리 알려주기\"}");
        var result = ai.needs("", new NeedsInput("일정이 바뀌었다", "미리 알려주기"));
        assertThat(result.requiresConfirmation()).isTrue();
        var input = ArgumentCaptor.forClass(String.class);
        verify(client).complete(contains("빈 값이면 감정을 새로 추정하지 않는다"), input.capture(), anyMap());
        var json = mapper.readTree(input.getValue());
        assertThat(json.size()).isEqualTo(3);
        assertThat(json.get("confirmedEmotion").asText()).isEmpty();
        assertThat(json.has("text")).isFalse();
        assertThat(json.get("newSituation").asText()).isEqualTo("일정이 바뀌었다");
    }

    @Test void 정리와_계획은_사용자_확정_내용만_직렬화한다() throws Exception {
        when(client.complete(anyString(), anyString(), anyMap()))
                .thenReturn("{\"summary\":\"이 사람은 일정이 미리 정해지길 원했다.\"}")
                .thenReturn("{\"ifSituation\":\"일정이 다시 바뀌면\",\"thenAction\":\"잠시 쉬고 가능한 시간을 묻는다\"}");
        var reflection = new Reflection("새 상황", "", "존중", "알려주기");
        assertThat(ai.summarize(reflection).requiresConfirmation()).isTrue();
        assertThat(ai.plan("내가 고친 정리", reflection).requiresConfirmation()).isTrue();
        var inputs = ArgumentCaptor.forClass(String.class);
        verify(client, times(2)).complete(anyString(), inputs.capture(), anyMap());
        assertThat(mapper.readTree(inputs.getAllValues().get(0)).get("emotion").asText()).isEmpty();
        assertThat(mapper.readTree(inputs.getAllValues().get(1)).get("confirmedSummary").asText()).isEqualTo("내가 고친 정리");
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "{}", "not-json", "{\"summary\":123}", "{\"summary\":\"\"}",
            "{\"summary\":\"정리\",\"extra\":\"노출 금지\"}", "{\"summary\":\"정리\"} {}"})
    void 잘못된_정리_결과를_노출하지_않는다(String content) {
        when(client.complete(anyString(), anyString(), anyMap())).thenReturn(content);
        assertThatThrownBy(() -> ai.summarize(new Reflection("상황", "", "", "")))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.AI_INVALID_RESPONSE));
    }

    @Test void 욕구중복과_너무긴_공감은_실패한다() {
        when(client.complete(anyString(), anyString(), anyMap()))
                .thenReturn("{\"needs\":[\"존중\",\"존중\"],\"desiredResponse\":\"\"}")
                .thenReturn("{\"empathy\":\"" + "가".repeat(201) + "\"}");
        assertThatThrownBy(() -> ai.needs("", new NeedsInput("상황", ""))).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> ai.empathize("상황")).isInstanceOf(BusinessException.class);
    }
}
