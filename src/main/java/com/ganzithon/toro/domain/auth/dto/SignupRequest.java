package com.ganzithon.toro.domain.auth.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 약관은 개인정보 수집·이용 동의 1개뿐이고 반드시 true여야 한다.
 * 비밀번호 확인 칸은 프론트에서만 비교한다.
 */
public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Pattern(regexp = UserRules.PASSWORD_REGEX, message = UserRules.PASSWORD_MESSAGE) String password,
        @NotNull @Pattern(regexp = UserRules.NAME_REGEX, message = UserRules.NAME_MESSAGE) String nickname,
        @NotNull @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다") Boolean agreePrivacy
) {
    public SignupRequest {
        email = UserRules.strip(email);
        nickname = UserRules.strip(nickname);
    }
}
