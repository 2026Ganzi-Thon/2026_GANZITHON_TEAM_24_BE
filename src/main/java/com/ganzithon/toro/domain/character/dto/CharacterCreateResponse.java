package com.ganzithon.toro.domain.character.dto;

/** 302 만들기 응답 { character: {...} } (명세대로 한 번 감싼다) */
public record CharacterCreateResponse(CharacterResponse character) {
}
