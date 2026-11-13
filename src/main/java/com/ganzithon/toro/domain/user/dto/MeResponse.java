package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.user.entity.User;
import java.time.LocalDateTime;

/** 201 내 정보, 203 수정 응답 */
public record MeResponse(Long id, String email, String nickname, String background, LocalDateTime createdAt) {

    public static MeResponse from(User user) {
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname(), user.getBackground(),
                user.getCreatedAt());
    }
}
