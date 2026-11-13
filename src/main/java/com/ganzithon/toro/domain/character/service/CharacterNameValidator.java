package com.ganzithon.toro.domain.character.service;

import com.ganzithon.toro.domain.user.UserRules;
import com.ganzithon.toro.global.exception.BusinessException;
import com.ganzithon.toro.global.exception.ErrorCode;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 캐릭터 이름 검사. 기획: "실명·연락처·SNS 계정은 입력할 수 없어요."
 * 1) 연락처·이메일·@아이디·URL 모양이면 CHAR_004 (실명은 서버가 알아낼 수 없어서 화면 안내로 막는다)
 * 2) 한글·영문·숫자 1~10자가 아니면 COMMON_001
 */
@Component
public class CharacterNameValidator {

    /** 구분자(- . 공백)를 빼고 숫자가 9개 이상이면 전화번호로 본다 (지역번호 포함 가장 짧은 번호가 9자리) */
    private static final int PHONE_MIN_DIGITS = 9;
    private static final Pattern URL_LIKE =
            Pattern.compile("(?i)(https?://|www\\.|\\.(com|net|org|kr|io|me|co)\\b)");

    public void validate(String name) {
        if (looksLikeContact(name)) {
            throw new BusinessException(ErrorCode.CHARACTER_NAME_HAS_CONTACT);
        }
        if (!name.matches(UserRules.NAME_REGEX)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "name: " + UserRules.NAME_MESSAGE);
        }
    }

    boolean looksLikeContact(String name) {
        if (name.contains("@") || URL_LIKE.matcher(name).find()) {
            return true;
        }
        long digits = name.chars().filter(Character::isDigit).count();
        return digits >= PHONE_MIN_DIGITS;
    }
}
