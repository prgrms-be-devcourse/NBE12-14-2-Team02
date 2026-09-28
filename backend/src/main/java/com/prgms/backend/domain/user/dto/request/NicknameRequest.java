package com.prgms.backend.domain.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record NicknameRequest(
        @Schema(description = "닉네임", example = "홍길동")
        @NotBlank
        String nickname
) {
}
