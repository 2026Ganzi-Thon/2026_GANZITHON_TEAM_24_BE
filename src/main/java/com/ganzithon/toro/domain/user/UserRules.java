package com.ganzithon.toro.domain.user;

import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * 계정 입력 규칙. 회원가입·마이페이지·캐릭터 이름이 같이 쓴다.
 */
public final class UserRules {

    /** 영문 1개 이상 + 숫자 1개 이상, 8~13자. 특수문자 허용, 공백 불가 */
    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[!-~]{8,13}$";
    public static final String PASSWORD_MESSAGE = "영문과 숫자를 포함해 8~13자여야 합니다";
    public static final int PASSWORD_MAX_LENGTH = 13;

    /** 닉네임·캐릭터 이름: 한글·영문·숫자 1~10자, 공백·특수문자 불가 */
    public static final String NAME_REGEX = "^[ㄱ-ㅎㅏ-ㅣ가-힣A-Za-z0-9]{1,10}$";
    public static final String NAME_MESSAGE = "한글·영문·숫자 1~10자여야 합니다";

    public static final int BACKGROUND_MAX_LENGTH = 20;

    private UserRules() {
    }

    public static String strip(String value) {
        return value == null ? null : value.strip();
    }

    /**
     * 중복 확인을 통과한 뒤 동시에 가입해 유니크 제약에 걸린 경우를 에러 코드로 바꾼다.
     * 이메일·닉네임 중복 판단은 DB collation(utf8mb4_unicode_ci) 기준이라 대소문자를 구분하지 않는다.
     */
    public static BusinessException duplicateError(DataIntegrityViolationException e) {
        String message = String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase();
        if (message.contains("uk_users_nickname")) {
            return new BusinessException(ErrorCode.NICKNAME_DUPLICATED);
        }
        if (message.contains("uk_users_email")) {
            return new BusinessException(ErrorCode.EMAIL_DUPLICATED);
        }
        throw e;
    }
}
