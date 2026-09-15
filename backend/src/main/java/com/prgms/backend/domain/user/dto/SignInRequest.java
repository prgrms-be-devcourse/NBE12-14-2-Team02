package com.prgms.backend.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SignInRequest(
        @Email @NotBlank String email,
        @NotBlank String nickname,
        @NotBlank String password,
        @NotBlank String confirmPassword
){
}