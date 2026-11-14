package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.character.dto.CharacterResponse;
import com.ganzithon.toro.domain.user.entity.User;
import com.ganzithon.toro.global.time.KoreaTime;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

/**
 * 201 내 정보, 203 수정 응답.
 *
 * @param recentCharacter 가장 최근에 만들었거나 사용한 캐릭터(마이페이지 프로필 이미지용). 없으면 null → 프론트가 기본 말랑이를 보여 준다
 */
public record MeResponse(
        Long id,
        String email,
        String nickname,
        @Schema(allowableValues = {"READING", "TERRACE", "KITCHEN", "STUDIO", "GREEN", "DEFAULT"}) String background,
        @Schema(example = "2026-11-15T21:24:31.123456+09:00") OffsetDateTime createdAt,
        CharacterResponse recentCharacter
) {
    public static MeResponse of(User user, CharacterResponse recentCharacter) {
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname(), user.getBackground(),
                KoreaTime.withOffset(user.getCreatedAt()), recentCharacter);
    }
}
