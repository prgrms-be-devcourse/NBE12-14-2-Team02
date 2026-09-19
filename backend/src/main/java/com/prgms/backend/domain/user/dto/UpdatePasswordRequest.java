package com.prgms.backend.domain.user.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdatePasswordRequest(
        @Schema(description = "현재 비밀번호", example = "Passw0rd!")
        @NotBlank
        String password,

        @Schema(
                description = "새 비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)",
                example = "NewPassw0rd!",
                pattern = SignUpRequest.PASSWORD_PATTERN
        )
        @Pattern(
                regexp = SignUpRequest.PASSWORD_PATTERN,
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다."
        )
        @NotBlank
        String newPassword
){}
