package com.ganzithon.toro.global.exception;

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
    CHARACTER_NAME_HAS_CONTACT(HttpStatus.BAD_REQUEST, "CHAR_004", "이름에 연락처·이메일·SNS 계정은 쓸 수 없어요."),

    // 음성 변환
    UNSUPPORTED_AUDIO(HttpStatus.BAD_REQUEST, "STT_001", "지원하지 않는 파일 형식입니다."),
    AUDIO_TOO_LARGE(HttpStatus.BAD_REQUEST, "STT_002", "녹음이 너무 길거나 파일이 너무 큽니다."),
    STT_FAILED(HttpStatus.BAD_GATEWAY, "STT_003", "음성을 글자로 바꾸지 못했습니다. 다시 녹음해 주세요."),
    STT_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "STT_004", "음성 인식 연결 설정이 필요합니다."),
    STT_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "STT_005", "음성 인식 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    STT_NO_SPEECH(HttpStatus.BAD_REQUEST, "STT_006", "인식된 말이 없습니다. 다시 녹음하거나 글로 입력해 주세요."),

    SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_001", "세션이 없거나 접근할 수 없습니다."),
    SESSION_CLOSED(HttpStatus.GONE, "SESSION_002", "종료되거나 만료된 세션입니다."),
    INVALID_SESSION_STAGE(HttpStatus.CONFLICT, "SESSION_003", "현재 단계가 변경되었습니다. 화면을 확인해 주세요."),
    SESSION_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "SESSION_004", "진행 중인 세션이 많습니다. 기존 세션을 종료해 주세요."),

    // 털어놓기
    INVALID_MESSAGE(HttpStatus.BAD_REQUEST, "CHAT_001", "글이 비어 있거나 너무 깁니다."),

    // AI 감정 후보 (노션 명세 공유 전 초안)
    AI_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "AI_001", "AI 연결 설정이 필요합니다."),
    AI_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "AI_002", "AI에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요."),
    AI_INVALID_RESPONSE(HttpStatus.BAD_GATEWAY, "AI_003", "AI 제안을 만들지 못했습니다. 직접 입력하거나 다시 시도해 주세요."),
    AI_BUSY(HttpStatus.SERVICE_UNAVAILABLE, "AI_004", "AI 요청이 많습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
