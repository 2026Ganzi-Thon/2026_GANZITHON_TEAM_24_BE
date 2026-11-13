package com.ganzithon.toro.domain.character.dto;

import java.util.List;

/** 301 목록 응답 { characters: [...] } */
public record CharacterListResponse(List<CharacterResponse> characters) {
}
