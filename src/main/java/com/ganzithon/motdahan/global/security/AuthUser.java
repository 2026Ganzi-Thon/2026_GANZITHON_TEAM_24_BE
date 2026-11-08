package com.ganzithon.motdahan.global.security;

/**
 * 토큰에서 꺼낸 로그인 사용자. 컨트롤러에서 @AuthenticationPrincipal AuthUser user 로 받는다.
 */
public record AuthUser(Long id, String nickname) {
}
