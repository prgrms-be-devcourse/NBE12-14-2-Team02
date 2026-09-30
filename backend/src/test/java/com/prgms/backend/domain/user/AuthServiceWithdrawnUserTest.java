package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.dto.request.LogInRequest;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.global.exception.custom.user.LoginFailException;
import com.prgms.backend.global.exception.custom.user.WithdrawUserException;
import com.prgms.backend.security.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceWithdrawnUserTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User withdrawnUser;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtTokenProvider = mock(JwtTokenProvider.class);

        authService = new AuthService(
            userRepository,
            passwordEncoder,
            jwtTokenProvider
        );

        withdrawnUser = new User("user@test.com", "탈퇴회원", "encoded-password");
        ReflectionTestUtils.setField(withdrawnUser, "id", 1L);
        withdrawnUser.updateStatus(UserStatus.WITHDRAWN);
        withdrawnUser.updateDeletedAt(LocalDateTime.now());

        when(userRepository.findByEmail("user@test.com"))
            .thenReturn(Optional.of(withdrawnUser));
    }

    @Test
    @DisplayName("탈퇴한 회원은 비밀번호가 맞아도 로그인할 수 없다")
    void loginWithdrawnUser() {

        // given
        when(passwordEncoder.matches("password", "encoded-password"))
            .thenReturn(true);

        // when & then
        assertThrows(
            WithdrawUserException.class,
            () -> authService.login(new LogInRequest("user@test.com", "password"))
        );

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        verify(jwtTokenProvider, never()).createRefreshToken(anyLong());
    }

    @Test
    @DisplayName("탈퇴한 회원이라도 비밀번호가 틀리면 탈퇴 여부를 알리지 않고 로그인 실패 처리한다")
    void loginWithdrawnUserWrongPassword() {

        // given
        when(passwordEncoder.matches("wrong", "encoded-password"))
            .thenReturn(false);

        // when & then
        assertThrows(
            LoginFailException.class,
            () -> authService.login(new LogInRequest("user@test.com", "wrong"))
        );
    }

    @Test
    @DisplayName("탈퇴한 회원은 토큰을 재발급받을 수 없다")
    void reissueWithdrawnUser() {

        // given
        when(jwtTokenProvider.validateRefreshToken("old-refresh-token"))
            .thenReturn(true);
        when(jwtTokenProvider.getRefreshUserId("old-refresh-token"))
            .thenReturn(1L);
        when(userRepository.findById(1L))
            .thenReturn(Optional.of(withdrawnUser));

        // when & then
        assertThrows(
            WithdrawUserException.class,
            () -> authService.reissue("old-refresh-token")
        );

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
    }
}
