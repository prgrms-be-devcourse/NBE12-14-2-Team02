package com.prgms.backend.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

public record LogInResponse(
        @Schema(description = "액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
){
}
