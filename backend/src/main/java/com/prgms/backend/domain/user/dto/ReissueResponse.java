package com.prgms.backend.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReissueResponse (
        @Schema(description = "재발급된 액세스 토큰", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken
){
}
