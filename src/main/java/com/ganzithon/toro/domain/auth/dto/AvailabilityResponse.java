package com.ganzithon.toro.domain.auth.dto;

/** 이메일·닉네임 중복 확인 결과. 쓸 수 있으면 true */
public record AvailabilityResponse(boolean available) {
}
