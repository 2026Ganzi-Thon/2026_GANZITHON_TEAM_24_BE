package com.ganzithon.toro.global.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * jwt.secret: 32바이트 이상 문자열 (환경변수 JWT_SECRET)
 * jwt.expiration: 토큰 유효 기간 (기본 7일, refresh token 없음)
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration expiration) {
}
