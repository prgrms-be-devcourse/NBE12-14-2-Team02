package com.prgms.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignUpRequest(
        @Schema(description = "이메일", example = "test@example.com")
        @Email
        @NotBlank
        String email,

        @Schema(description = "닉네임", example = "홍길동")
        @NotBlank
        String nickname,

        @Schema(
                description = "비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)",
                example = "Passw0rd!",
                pattern = PASSWORD_PATTERN
        )
        @Pattern(
                regexp = PASSWORD_PATTERN,
                message = "비밀번호는 영문, 숫자, 특수문자를 포함해 8자 이상이어야 합니다."
        )
        @NotBlank
        String password,

        @Schema(description = "비밀번호 확인", example = "Passw0rd!")
        @NotBlank
        String confirmPassword
) {
    // 영문 1자 이상 + 숫자 1자 이상 + 특수문자 1자 이상 + 총 8자 이상
    static final String PASSWORD_PATTERN =
            "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!@#$%^&*()_+\\-=]).{8,}$";
}