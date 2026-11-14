package com.ganzithon.toro.domain.character.dto;

import com.ganzithon.toro.domain.character.entity.UserCharacter;
import com.ganzithon.toro.global.time.KoreaTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

public record CharacterResponse(
        Long id,
        String name,
        String color,
        String eyes,
        @Schema(description = "없으면 \"\"") String nose,
        String mouth,
        String eyebrows,
        @Schema(description = "없으면 \"\"") String accessory,
        @Schema(example = "2026-11-15T21:24:31.123456+09:00") OffsetDateTime lastUsedAt,
        @Schema(example = "2026-11-15T21:24:31.123456+09:00") OffsetDateTime updatedAt
) {
    public static CharacterResponse from(UserCharacter c) {
        return new CharacterResponse(c.getId(), c.getName(), c.getColor(), c.getEyes(), emptyIfNull(c.getNose()), c.getMouth(),
                c.getEyebrows(), emptyIfNull(c.getAccessory()),
                KoreaTime.withOffset(c.getLastUsedAt()), KoreaTime.withOffset(c.getUpdatedAt()));
    }

    /** 코·악세서리 선택 안 함은 프론트와 ""로 주고받는다 */
    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
