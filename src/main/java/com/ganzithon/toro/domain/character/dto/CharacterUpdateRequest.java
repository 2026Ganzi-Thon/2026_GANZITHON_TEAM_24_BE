package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.CharacterLook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 다시 꾸미기. 규칙은 CharacterCreateRequest와 같다 */
public record CharacterUpdateRequest(
        @Schema(example = "뭉이") @NotBlank String name,
        @Schema(allowableValues = {"PEACH", "PINK", "YELLOW", "GRAY", "BLUE", "MINT", "LAVENDER"})
        @NotBlank String color,
        @Schema(allowableValues = {"STYLE_1", "STYLE_2", "STYLE_3", "STYLE_4", "STYLE_5", "STYLE_6"})
        @NotBlank String eyes,
        @Schema(description = "\"\" 또는 null이면 선택 안 함",
                allowableValues = {"STYLE_1", "STYLE_2", "STYLE_3", "STYLE_4", "STYLE_5", "STYLE_6", ""})
        String nose,
        @Schema(allowableValues = {"STYLE_1", "STYLE_2", "STYLE_3", "STYLE_4", "STYLE_5", "STYLE_6"})
        @NotBlank String mouth,
        @Schema(allowableValues = {"STYLE_1", "STYLE_2", "STYLE_3", "STYLE_4", "STYLE_5", "STYLE_6"})
        @NotBlank String eyebrows,
        @Schema(description = "\"\" 또는 null이면 없음",
                allowableValues = {"STYLE_1", "STYLE_2", "STYLE_3", "STYLE_4", "STYLE_5", "STYLE_6", "NIGHTCAP", ""})
        String accessory
) implements CharacterLook {

    public CharacterUpdateRequest {
        name = CharacterCreateRequest.strip(name);
        color = CharacterCreateRequest.strip(color);
        eyes = CharacterCreateRequest.strip(eyes);
        nose = CharacterCreateRequest.blankToNull(nose);
        mouth = CharacterCreateRequest.strip(mouth);
        eyebrows = CharacterCreateRequest.strip(eyebrows);
        accessory = CharacterCreateRequest.blankToNull(accessory);
    }
}
