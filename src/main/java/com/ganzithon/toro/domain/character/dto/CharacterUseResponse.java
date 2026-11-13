package com.ganzithon.toro.domain.character.dto;

import java.time.LocalDateTime;

/** 305 사용 표시 응답 */
public record CharacterUseResponse(Long id, LocalDateTime lastUsedAt) {
}
