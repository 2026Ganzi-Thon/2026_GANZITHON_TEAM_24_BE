package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.CharacterLook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 꾸미기 항목은 모두 1개만 고른다. 선택지가 확정되기 전이라 20자 이내 문자면 그대로 저장한다(CHAR_002 검사는 꺼 둠).
 * 눈·코·입·눈썹·색상은 반드시 하나씩 고른다. 장신구만 안 골라도 되고 그때는 null(빈 문자열도 null로 본다).
 * 이름 규칙(한글·영문·숫자 1~10자, 연락처 금지)은 CharacterNameValidator가 검사한다.
 *
 * @param replaceId 이미 3개일 때 지우고 바꿀 내 캐릭터 id. 3개 미만이면 무시
 */
public record CharacterCreateRequest(
        @NotBlank String name,
        @NotBlank @Size(max = 20) String color,
        @NotBlank @Size(max = 20) String eyes,
        @NotBlank @Size(max = 20) String nose,
        @NotBlank @Size(max = 20) String mouth,
        @NotBlank @Size(max = 20) String eyebrows,
        @Size(max = 20) String accessory,
        Long replaceId
) implements CharacterLook {

    public CharacterCreateRequest {
        name = strip(name);
        color = strip(color);
        eyes = strip(eyes);
        nose = strip(nose);
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
