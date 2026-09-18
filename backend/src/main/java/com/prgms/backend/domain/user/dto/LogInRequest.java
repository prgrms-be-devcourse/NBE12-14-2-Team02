package com.prgms.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LogInRequest(

        @Schema(
                description = "이메일",
                example = "test@example.com"
        )
        @NotBlank
        @Email
        String email,

        @Schema(
                description = "비밀번호",
                example = "Passw0rd!"
        )
        @NotBlank
        String password
) {
}
