package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.CharacterLook;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 꾸미기 항목은 모두 1개만 고른다. 허용 값은 CharacterOptions (없는 값이면 CHAR_002).
 * - 눈·입·눈썹·색상: 반드시 하나
 * - 코·악세서리: 해제하면 ""(또는 null) → DB에는 null, 응답은 ""
 * 이름 규칙(한글·영문·숫자 1~10자, 연락처 금지)은 CharacterNameValidator가 검사한다.
 *
 * @param replaceId 이미 3개일 때 지우고 바꿀 내 캐릭터 id. 3개 미만이면 무시
 */
public record CharacterCreateRequest(
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
        String accessory,
        Long replaceId
) implements CharacterLook {

    public CharacterCreateRequest {
        name = strip(name);
        color = strip(color);
        eyes = strip(eyes);
        nose = blankToNull(nose);
        mouth = strip(mouth);
        eyebrows = strip(eyebrows);
        accessory = blankToNull(accessory);
    }

    static String strip(String value) {
        return value == null ? null : value.strip();
    }

    static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
