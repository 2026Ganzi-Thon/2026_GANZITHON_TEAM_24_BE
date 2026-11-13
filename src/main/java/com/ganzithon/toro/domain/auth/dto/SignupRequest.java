package com.ganzithon.toro.domain.auth.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 필수 약관 2개(이용약관, 개인정보 수집·이용)는 모두 true여야 한다. 선택 약관 1개는 미확정이라 아직 받지 않는다.
 * 비밀번호 확인 칸은 프론트에서만 비교한다.
 */
public record SignupRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Pattern(regexp = UserRules.PASSWORD_REGEX, message = UserRules.PASSWORD_MESSAGE) String password,
        @NotNull @Pattern(regexp = UserRules.NAME_REGEX, message = UserRules.NAME_MESSAGE) String nickname,
        @NotNull @AssertTrue(message = "이용약관에 동의해야 합니다") Boolean agreeTerms,
        @NotNull @AssertTrue(message = "개인정보 수집·이용에 동의해야 합니다") Boolean agreePrivacy
) {
    public SignupRequest {
        email = UserRules.strip(email);
        nickname = UserRules.strip(nickname);
    }
}
