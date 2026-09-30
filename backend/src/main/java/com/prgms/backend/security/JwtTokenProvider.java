package com.prgms.backend.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtTokenProvider {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;

    public JwtTokenProvider(
            @Qualifier("accessSecretKey") SecretKey accessSecretKey,
            @Qualifier("refreshSecretKey") SecretKey refreshSecretKey
    ) {
        if (java.util.Arrays.equals(accessSecretKey.getEncoded(), refreshSecretKey.getEncoded())) {
            throw new IllegalArgumentException("Access와 refresh JWT key는 달라야 합니다.");
        }
        this.accessSecretKey = accessSecretKey;
        this.refreshSecretKey = refreshSecretKey;
    }

    @Value("${custom.jwt.access-token-validity-seconds}")
    private long accessExpiration;

    @Value("${custom.jwt.refresh-token-validity-seconds}")
    private long refreshExpiration;

    public String createAccessToken(
            Long userId
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(accessSecretKey)
                .compact();
    }

    public String createRefreshToken(
            Long userId
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration*1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(refreshSecretKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessSecretKey);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, refreshSecretKey);
    }

    private boolean validateToken(String token, SecretKey secretKey) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public Long getAccessUserId(String token) {
        return getUserId(token, accessSecretKey);
    }

    public Long getRefreshUserId(String token) {
        return getUserId(token, refreshSecretKey);
    }

    private Long getUserId(String token, SecretKey secretKey) {
        String subject = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.parseLong(subject);
    }
}
