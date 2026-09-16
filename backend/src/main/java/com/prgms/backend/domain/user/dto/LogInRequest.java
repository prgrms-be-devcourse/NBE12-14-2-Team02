package com.prgms.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LogInRequest(
        @NotBlank
        @Email
        String email,

        @NotBlank
        String password
) {
}
