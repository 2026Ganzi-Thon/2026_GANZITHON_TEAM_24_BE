package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.UserCharacter;
import java.time.LocalDateTime;

public record CharacterResponse(
        Long id,
        String name,
        String color,
        String eyes,
        String nose,
        String mouth,
        String eyebrows,
        String accessory,
        LocalDateTime lastUsedAt,
        LocalDateTime updatedAt
) {
    public static CharacterResponse from(UserCharacter c) {
        return new CharacterResponse(c.getId(), c.getName(), c.getColor(), c.getEyes(), c.getNose(), c.getMouth(),
                c.getEyebrows(), c.getAccessory(), c.getLastUsedAt(), c.getUpdatedAt());
    }
}
