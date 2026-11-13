package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 205 비밀번호 변경. 새 비밀번호 규칙은 회원가입과 같다 */
public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotNull @Pattern(regexp = UserRules.PASSWORD_REGEX, message = UserRules.PASSWORD_MESSAGE) String newPassword
) {
}
