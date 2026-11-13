package com.ganzithon.toro.global.security;

/**
 * 토큰에서 꺼낸 로그인 사용자. 컨트롤러에서 @AuthenticationPrincipal AuthUser user 로 받는다.
 * 닉네임은 마이페이지에서 바뀔 수 있어서 토큰에 넣지 않는다. 필요하면 UserService.getNickname(id)로 DB에서 읽는다.
 */
public record AuthUser(Long id) {
}
