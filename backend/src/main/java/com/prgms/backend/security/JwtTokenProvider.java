package com.prgms.backend.security;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtTokenProvider {

    private final SecretKey accessSecretKey;
    private final SecretKey refreshSecretKey;
    private final Clock clock;

    @Autowired
    public JwtTokenProvider(
            @Qualifier("accessSecretKey") SecretKey accessSecretKey,
            @Qualifier("refreshSecretKey") SecretKey refreshSecretKey
    ) {
        this(accessSecretKey, refreshSecretKey, Clock.systemUTC());
    }

    public JwtTokenProvider(SecretKey accessSecretKey, SecretKey refreshSecretKey, Clock clock) {
        if (java.util.Arrays.equals(accessSecretKey.getEncoded(), refreshSecretKey.getEncoded())) {
            throw new IllegalArgumentException("Access와 refresh JWT key는 달라야 합니다.");
        }
        this.accessSecretKey = accessSecretKey;
        this.refreshSecretKey = refreshSecretKey;
        this.clock = clock;
    }

    @Value("${custom.jwt.access-token-validity-seconds}")
    private long accessExpiration;

    @Value("${custom.jwt.refresh-token-validity-seconds}")
    private long refreshExpiration;

    public String createAccessToken(
            Long userId
    ) {
        Date now = Date.from(clock.instant());
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
        Date now = Date.from(clock.instant());
        Date expiryDate = new Date(now.getTime() + refreshExpiration*1000);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .id(UUID.randomUUID().toString())   // jti 추가
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
                    .clock(() -> Date.from(clock.instant()))
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
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.parseLong(subject);
    }
}
