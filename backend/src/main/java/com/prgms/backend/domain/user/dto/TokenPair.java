package com.prgms.backend.domain.user.dto;

public record TokenPair (
        String accessToken,
        String refreshToken
) {
}
