package com.ganzithon.toro.domain.vent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record EmotionSuggestionResponse(
        @Schema(description = "상대방의 의도를 추측하지 않는 짧은 공감") String empathy,
        @Schema(description = "확정되지 않은 감정 후보 2~3개") List<String> candidates,
        @Schema(description = "감정을 선택하지 않아도 다음 단계로 진행 가능") boolean canSkip
) {
    public EmotionSuggestionResponse {
        candidates = List.copyOf(candidates);
    }

    @Override
    public String toString() {
        return "EmotionSuggestionResponse[redacted]";
    }
}
