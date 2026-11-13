package com.ganzithon.motdahan.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드는 이 파일 하나에서 관리한다. 노션 명세서의 에러 코드 표와 맞춘다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "입력값 형식이 올바르지 않습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "COMMON_002", "로그인이 필요합니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_003", "서버 오류가 발생했습니다."),

    // 인증
    EMAIL_DUPLICATED(HttpStatus.CONFLICT, "AUTH_001", "이미 가입된 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "AUTH_002", "이메일 또는 비밀번호가 틀렸습니다."),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "AUTH_003", "현재 비밀번호가 틀렸습니다."),
    NICKNAME_DUPLICATED(HttpStatus.CONFLICT, "AUTH_004", "이미 사용 중인 닉네임입니다."),

    // 캐릭터
    CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAR_001", "캐릭터를 찾을 수 없습니다."),
    INVALID_CHARACTER_OPTION(HttpStatus.BAD_REQUEST, "CHAR_002", "허용되지 않은 꾸미기 값입니다."),
    CHARACTER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "CHAR_003", "캐릭터는 최대 3개예요. 교체할 캐릭터를 골라 주세요."),

    // 음성 변환
    UNSUPPORTED_AUDIO(HttpStatus.BAD_REQUEST, "STT_001", "지원하지 않는 파일 형식입니다."),
    AUDIO_TOO_LARGE(HttpStatus.BAD_REQUEST, "STT_002", "녹음이 너무 길거나 파일이 너무 큽니다."),
    STT_FAILED(HttpStatus.BAD_GATEWAY, "STT_003", "음성을 글자로 바꾸지 못했습니다. 다시 녹음해 주세요."),

    // 털어놓기
    INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_001", "글이 비어 있거나 너무 깁니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
