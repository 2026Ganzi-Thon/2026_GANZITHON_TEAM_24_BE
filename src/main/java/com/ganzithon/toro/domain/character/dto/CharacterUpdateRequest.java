package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.CharacterLook;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 다시 꾸미기. 규칙은 CharacterCreateRequest와 같다 */
public record CharacterUpdateRequest(
        @NotBlank String name,
        @NotBlank @Size(max = 20) String color,
        @NotBlank @Size(max = 20) String eyes,
        @NotBlank @Size(max = 20) String nose,
        @NotBlank @Size(max = 20) String mouth,
        @NotBlank @Size(max = 20) String eyebrows,
        @Size(max = 20) String accessory
) implements CharacterLook {

    public CharacterUpdateRequest {
        name = CharacterCreateRequest.strip(name);
        color = CharacterCreateRequest.strip(color);
        eyes = CharacterCreateRequest.strip(eyes);
        nose = CharacterCreateRequest.strip(nose);
        mouth = CharacterCreateRequest.strip(mouth);
        eyebrows = CharacterCreateRequest.strip(eyebrows);
        accessory = CharacterCreateRequest.blankToNull(accessory);
    }
}
