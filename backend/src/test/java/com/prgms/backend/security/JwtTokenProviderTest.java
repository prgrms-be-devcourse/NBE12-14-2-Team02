package com.prgms.backend.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
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
    void accessTokenIsAcceptedOnlyByAccessKey() {
        String token = provider.createAccessToken(1L);

        assertTrue(provider.validateAccessToken(token));
        assertEquals(1L, provider.getAccessUserId(token));
        assertFalse(provider.validateRefreshToken(token));
        assertThrows(JwtException.class, () -> provider.getRefreshUserId(token));
    }

    @Test
    void refreshTokenIsAcceptedOnlyByRefreshKey() {
        String token = provider.createRefreshToken(1L);

        assertTrue(provider.validateRefreshToken(token));
        assertEquals(1L, provider.getRefreshUserId(token));
        assertFalse(provider.validateAccessToken(token));
        assertThrows(JwtException.class, () -> provider.getAccessUserId(token));
    }

    @Test
    void expiredTokensAreRejected() {
        ReflectionTestUtils.setField(provider, "accessExpiration", -60L);
        ReflectionTestUtils.setField(provider, "refreshExpiration", -60L);

        assertFalse(provider.validateAccessToken(provider.createAccessToken(1L)));
        assertFalse(provider.validateRefreshToken(provider.createRefreshToken(1L)));
    }

    @Test
    void missingAndMalformedTokensAreRejected() {
        assertFalse(provider.validateAccessToken(null));
        assertFalse(provider.validateRefreshToken(null));
        assertFalse(provider.validateAccessToken("invalid-token"));
        assertFalse(provider.validateRefreshToken("invalid-token"));
    }

    @Test
    void identicalKeysAreRejected() {
        var key = Jwts.SIG.HS256.key().build();
        assertThrows(IllegalArgumentException.class, () -> new JwtTokenProvider(key, key));
    }
}
