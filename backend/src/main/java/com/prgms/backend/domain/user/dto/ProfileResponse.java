package com.prgms.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProfileResponse (
        @Schema(description = "이메일", example = "test@example.com")
        String email,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname
) {
}
