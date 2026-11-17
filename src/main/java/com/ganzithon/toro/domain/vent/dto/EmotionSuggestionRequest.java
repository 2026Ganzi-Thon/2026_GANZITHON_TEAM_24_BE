package com.ganzithon.toro.domain.vent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EmotionSuggestionRequest(
        @Schema(description = "감정 후보를 제안받을 하소연 텍스트. 1~2000자",
                example = "친구가 약속을 계속 미뤄서 내 시간이 존중받지 못한 것 같아요.")
        @NotBlank @Size(max = 2000) String text
) {
    public EmotionSuggestionRequest {
        text = text == null ? null : text.strip();
    }

    @Override
    public String toString() {
        return "EmotionSuggestionRequest[redacted]";
    }
}
