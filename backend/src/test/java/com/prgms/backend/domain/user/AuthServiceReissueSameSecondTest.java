package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.global.exception.custom.user.ReissueFailException;
import com.prgms.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceReissueSameSecondTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);
    private SecretKey refreshKey;

    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        // 토큰 문자열 자체를 비교해야 하므로 JwtTokenProvider는 실제 객체를 사용
        refreshKey = Jwts.SIG.HS256.key().build();
        jwtTokenProvider = new JwtTokenProvider(
                Jwts.SIG.HS256.key().build(), refreshKey, CLOCK);
        ReflectionTestUtils.setField(jwtTokenProvider, "accessExpiration", 1800L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshExpiration", 1209600L);

        authService = new AuthService(
                userRepository,
                mock(PasswordEncoder.class),
                jwtTokenProvider
        );

        user = new User("user@test.com", "회원", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("같은 초 안에 같은 회원의 refresh token을 두 번 만들어도 서로 다른 토큰이 생성된다")
    void createRefreshTokenInSameSecond() {

        // when
        String first = jwtTokenProvider.createRefreshToken(1L);
        String second = jwtTokenProvider.createRefreshToken(1L);

        // then
        assertNotEquals(first, second);
        assertSameIssueTime(first, second);
        assertNotNull(claims(first).getId());
        assertNotEquals(claims(first).getId(), claims(second).getId());
    }

    @Test
    @DisplayName("발급과 같은 초 안에 재발급해도 기존과 다른 refresh token이 재발급된다")
    void reissueInSameSecond() {

        // given
        String oldRefreshToken = jwtTokenProvider.createRefreshToken(1L);
        user.updateRefreshToken(oldRefreshToken);

        // when
        TokenPair reissued = authService.reissue(oldRefreshToken);

        // then
        assertNotEquals(oldRefreshToken, reissued.refreshToken());
        assertEquals(reissued.refreshToken(), user.getRefreshToken());
        assertSameIssueTime(oldRefreshToken, reissued.refreshToken());
        assertTrue(jwtTokenProvider.validateAccessToken(reissued.accessToken()));
        assertEquals(1L, jwtTokenProvider.getAccessUserId(reissued.accessToken()));
    }

    @Test
    @DisplayName("같은 초 안에 재발급해도 이미 사용한 refresh token으로는 다시 재발급받을 수 없다")
    void reuseOldRefreshTokenAfterReissueInSameSecond() {

        // given
        String oldRefreshToken = jwtTokenProvider.createRefreshToken(1L);
        user.updateRefreshToken(oldRefreshToken);
        TokenPair reissued = authService.reissue(oldRefreshToken);

        // when & then
        assertThrows(ReissueFailException.class, () -> authService.reissue(oldRefreshToken));
        assertEquals(reissued.refreshToken(), user.getRefreshToken());
        assertDoesNotThrow(() -> authService.reissue(reissued.refreshToken()));
    }

    private void assertSameIssueTime(String first, String second) {
        assertEquals(CLOCK.instant(), claims(first).getIssuedAt().toInstant());
        assertEquals(claims(first).getIssuedAt(), claims(second).getIssuedAt());
        assertEquals(claims(first).getExpiration(), claims(second).getExpiration());
    }

    private Claims claims(String token) {
        return Jwts.parser().verifyWith(refreshKey)
            .clock(() -> java.util.Date.from(CLOCK.instant()))
            .build().parseSignedClaims(token).getPayload();
    }
}
