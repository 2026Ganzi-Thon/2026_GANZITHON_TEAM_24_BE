package com.ganzithon.toro.domain.auth.dto;

import com.ganzithon.toro.domain.user.UserRules;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {

    public LoginRequest {
        email = UserRules.strip(email);
    }
}
