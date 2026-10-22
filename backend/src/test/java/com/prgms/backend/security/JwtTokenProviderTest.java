package com.prgms.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setUp() {
        provider = new JwtTokenProvider(
                Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256.key().build());
        ReflectionTestUtils.setField(provider, "accessExpiration", 1800L);
        ReflectionTestUtils.setField(provider, "refreshExpiration", 1209600L);
    }

    @Test
    @DisplayName("액세스 토큰은 액세스 키로만 검증하고 회원 ID를 조회할 수 있다")
    void accessTokenIsAcceptedOnlyByAccessKey() {
        String token = provider.createAccessToken(1L);

        assertTrue(provider.validateAccessToken(token));
        assertEquals(1L, provider.getAccessUserId(token));
        assertFalse(provider.validateRefreshToken(token));
        assertThrows(JwtException.class, () -> provider.getRefreshUserId(token));
    }

    @Test
    @DisplayName("리프레시 토큰은 리프레시 키로만 검증하고 회원 ID를 조회할 수 있다")
    void refreshTokenIsAcceptedOnlyByRefreshKey() {
        String token = provider.createRefreshToken(1L);

        assertTrue(provider.validateRefreshToken(token));
        assertEquals(1L, provider.getRefreshUserId(token));
        assertFalse(provider.validateAccessToken(token));
        assertThrows(JwtException.class, () -> provider.getAccessUserId(token));
    }

    @Test
    @DisplayName("만료된 액세스 토큰과 리프레시 토큰은 검증에 실패한다")
    void expiredTokensAreRejected() {
        ReflectionTestUtils.setField(provider, "accessExpiration", -60L);
        ReflectionTestUtils.setField(provider, "refreshExpiration", -60L);

        assertFalse(provider.validateAccessToken(provider.createAccessToken(1L)));
        assertFalse(provider.validateRefreshToken(provider.createRefreshToken(1L)));
    }

    @Test
    @DisplayName("토큰이 없거나 형식이 올바르지 않으면 검증에 실패한다")
    void missingAndMalformedTokensAreRejected() {
        assertFalse(provider.validateAccessToken(null));
        assertFalse(provider.validateRefreshToken(null));
        assertFalse(provider.validateAccessToken("invalid-token"));
        assertFalse(provider.validateRefreshToken("invalid-token"));
    }

    @Test
    @DisplayName("액세스 토큰과 리프레시 토큰에 같은 서명 키를 사용할 수 없다")
    void identicalKeysAreRejected() {
        var key = Jwts.SIG.HS256.key().build();
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(key, key));
    }
}
