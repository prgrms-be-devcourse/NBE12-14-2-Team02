package com.prgms.backend.security;

import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.secret")
    private SecretKey secret;

    @Value("{jwt.access-token-validity-seconds")
    private long accessExpiration;

    @Value("{jwt.refresh-token-validity-seconds")
    private long refreshExpiration;

    public String createAccessToken(
            Long userId
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secret)
                .compact();
    }

    public String createRefreshToken(
            Long userId
    ) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secret)
                .compact();
    }

    public Boolean validateToken(String token){
        try {
            Jwts.parser()
                    .verifyWith(secret)
                    .build()
                    .parseSignedClaims(token);

            return true;
        } catch (Exception e) {
            return false;
        }
    }


    public Long getUserId(String token) {
        String subject = Jwts.parser()
                .verifyWith(secret)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();

        return Long.parseLong(subject);
    }
}
