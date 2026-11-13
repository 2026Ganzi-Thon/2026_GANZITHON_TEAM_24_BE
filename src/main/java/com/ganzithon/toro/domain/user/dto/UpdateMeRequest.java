package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 203 닉네임·홈 배경 수정. 보낸 필드만 바꾼다(null이면 그대로).
 * 배경은 홈 화면 배경 선택지 이름. 선택지 미정이라 20자 이내 문자면 그대로 저장한다.
 */
public record UpdateMeRequest(
        @Pattern(regexp = UserRules.NAME_REGEX, message = UserRules.NAME_MESSAGE) String nickname,
        @Size(min = 1, max = UserRules.BACKGROUND_MAX_LENGTH) String background
) {
    public UpdateMeRequest {
        nickname = UserRules.strip(nickname);
        background = UserRules.strip(background);
    }
}
