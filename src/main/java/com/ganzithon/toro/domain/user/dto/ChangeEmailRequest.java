package com.ganzithon.toro.domain.user.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 204 아이디(이메일) 변경. 본인 확인용 현재 비밀번호 필요 */
public record ChangeEmailRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank String currentPassword
) {
    public ChangeEmailRequest {
        email = UserRules.strip(email);
    }
}
