package com.ganzithon.toro.domain.auth.dto;

/**
 * @param expiresIn   토큰 유효 시간(초)
 * @param hasCharacter 저장된 캐릭터가 1개 이상이면 true (홈 표시용. 없어도 홈으로 간다)
 */
public record LoginResponse(String accessToken, long expiresIn, LoginUser user, boolean hasCharacter) {

    public record LoginUser(Long id, String nickname) {
    }
}
