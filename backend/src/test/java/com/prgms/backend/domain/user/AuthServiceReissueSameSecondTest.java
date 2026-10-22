package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.global.exception.custom.user.ReissueFailException;
import com.prgms.backend.security.JwtTokenProvider;
import io.jsonwebtoken.Jwts;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceReissueSameSecondTest {

    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        // 토큰 문자열 자체를 비교해야 하므로 JwtTokenProvider는 실제 객체를 사용
        jwtTokenProvider = new JwtTokenProvider(
                Jwts.SIG.HS256.key().build(), Jwts.SIG.HS256.key().build());
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

        // 첫 JWT 생성은 클래스 로딩 때문에 느려서 같은 초 안에 끝나지 않을 수 있으므로 미리 한 번 생성
        jwtTokenProvider.createRefreshToken(1L);
    }

    @Test
    @DisplayName("같은 초 안에 같은 회원의 refresh token을 두 번 만들어도 서로 다른 토큰이 생성된다")
    void createRefreshTokenInSameSecond() throws InterruptedException {

        // given
        waitUntilStartOfNextSecond();

        // when
        String first = jwtTokenProvider.createRefreshToken(1L);
        String second = jwtTokenProvider.createRefreshToken(1L);

        // then
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("발급과 같은 초 안에 재발급해도 기존과 다른 refresh token이 재발급된다")
    void reissueInSameSecond() throws InterruptedException {

        // given
        waitUntilStartOfNextSecond();
        String oldRefreshToken = jwtTokenProvider.createRefreshToken(1L);
        user.updateRefreshToken(oldRefreshToken);

        // when
        TokenPair reissued = authService.reissue(oldRefreshToken);

        // then
        assertNotEquals(oldRefreshToken, reissued.refreshToken());
        assertNotEquals(oldRefreshToken, user.getRefreshToken());
    }

    @Test
    @DisplayName("같은 초 안에 재발급해도 이미 사용한 refresh token으로는 다시 재발급받을 수 없다")
    void reuseOldRefreshTokenAfterReissueInSameSecond() throws InterruptedException {

        // given
        waitUntilStartOfNextSecond();
        String oldRefreshToken = jwtTokenProvider.createRefreshToken(1L);
        user.updateRefreshToken(oldRefreshToken);
        authService.reissue(oldRefreshToken);

        // when & then
        assertThrows(ReissueFailException.class, () -> authService.reissue(oldRefreshToken));
    }

    // JWT의 iat/exp는 초 단위라서, 초가 바뀐 직후에 토큰을 만들어야 같은 초 안에서 생성됨을 보장할 수 있다
    private void waitUntilStartOfNextSecond() throws InterruptedException {
        long now = System.currentTimeMillis();
        Thread.sleep(1000 - now % 1000);
    }
}
