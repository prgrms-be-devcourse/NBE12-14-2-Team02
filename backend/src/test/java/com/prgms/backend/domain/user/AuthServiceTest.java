package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.dto.request.LogInRequest;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.global.exception.custom.user.DuplicateEmailNickname;
import com.prgms.backend.global.exception.custom.user.LoginFailException;
import com.prgms.backend.global.exception.custom.user.PasswordMismatchException;
import com.prgms.backend.global.exception.custom.user.ReissueFailException;
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import com.prgms.backend.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;

    private AuthService authService;

    private User user;

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

        user = new User("user@test.com", "회원", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);
        user.updateRefreshToken("refresh-token");
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 일치하면 예외가 발생하지 않는다")
    void validateConfirmPassword() {

        // when & then
        assertDoesNotThrow(
            () -> authService.validateConfirmPassword("Passw0rd!", "Passw0rd!")
        );
    }

    @Test
    @DisplayName("비밀번호와 비밀번호 확인이 다르면 예외가 발생한다")
    void validateConfirmPasswordMismatch() {

        // when & then
        assertThrows(
            PasswordMismatchException.class,
            () -> authService.validateConfirmPassword("Passw0rd!", "Different0!")
        );
    }

    @Test
    @DisplayName("회원가입 시 비밀번호를 암호화해 저장하고 회원 id를 반환한다")
    void signup() {

        // given
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByNickname("새회원")).thenReturn(false);
        when(passwordEncoder.encode("Passw0rd!")).thenReturn("encoded-Passw0rd!");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });

        // when
        Long userId = authService.signup("new@test.com", "새회원", "Passw0rd!", "Passw0rd!");

        // then
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User saved = captor.getValue();
        assertEquals(2L, userId);
        assertEquals("new@test.com", saved.getEmail());
        assertEquals("새회원", saved.getNickname());
        assertEquals("encoded-Passw0rd!", saved.getPassword());
    }

    @Test
    @DisplayName("비밀번호 확인이 일치하지 않으면 회원가입할 수 없다")
    void signupPasswordMismatch() {

        // when & then
        assertThrows(
            PasswordMismatchException.class,
            () -> authService.signup("new@test.com", "새회원", "Passw0rd!", "Different0!")
        );

        verify(userRepository, never()).existsByEmail(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이미 사용 중인 이메일로는 회원가입할 수 없다")
    void signupDuplicateEmail() {

        // given
        when(userRepository.existsByEmail("user@test.com")).thenReturn(true);

        // when & then
        assertThrows(
            DuplicateEmailNickname.class,
            () -> authService.signup("user@test.com", "새회원", "Passw0rd!", "Passw0rd!")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로는 회원가입할 수 없다")
    void signupDuplicateNickname() {

        // given
        when(userRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(userRepository.existsByNickname("회원")).thenReturn(true);

        // when & then
        assertThrows(
            DuplicateEmailNickname.class,
            () -> authService.signup("new@test.com", "회원", "Passw0rd!", "Passw0rd!")
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("로그인에 성공하면 토큰을 발급하고 refresh token을 저장한다")
    void login() {

        // given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");

        // when
        TokenPair tokenPair = authService.login(new LogInRequest("user@test.com", "password"));

        // then
        assertEquals("new-access-token", tokenPair.accessToken());
        assertEquals("new-refresh-token", tokenPair.refreshToken());
        assertEquals("new-refresh-token", user.getRefreshToken());
    }

    @Test
    @DisplayName("가입되지 않은 이메일로는 로그인할 수 없다")
    void loginUserNotFound() {

        // given
        when(userRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> authService.login(new LogInRequest("none@test.com", "password"))
        );
    }

    @Test
    @DisplayName("비밀번호가 틀리면 로그인할 수 없다")
    void loginWrongPassword() {

        // given
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // when & then
        assertThrows(
            LoginFailException.class,
            () -> authService.login(new LogInRequest("user@test.com", "wrong"))
        );

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        assertEquals("refresh-token", user.getRefreshToken());
    }

    @Test
    @DisplayName("저장된 refresh token과 일치하면 토큰을 재발급하고 refresh token을 교체한다")
    void reissue() {

        // given
        givenValidRefreshToken("refresh-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh-token");

        // when
        TokenPair tokenPair = authService.reissue("refresh-token");

        // then
        assertEquals("new-access-token", tokenPair.accessToken());
        assertEquals("new-refresh-token", tokenPair.refreshToken());
        assertEquals("new-refresh-token", user.getRefreshToken());
    }

    @Test
    @DisplayName("유효하지 않은 refresh token으로는 재발급받을 수 없다")
    void reissueInvalidToken() {

        // given
        when(jwtTokenProvider.validateRefreshToken("invalid-token")).thenReturn(false);

        // when & then
        assertThrows(
            ReissueFailException.class,
            () -> authService.reissue("invalid-token")
        );

        verify(userRepository, never()).findById(anyLong());
        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
    }

    @Test
    @DisplayName("저장된 refresh token과 다르면 재발급받을 수 없다")
    void reissueTokenMismatch() {

        // given
        givenValidRefreshToken("old-refresh-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThrows(
            ReissueFailException.class,
            () -> authService.reissue("old-refresh-token")
        );

        verify(jwtTokenProvider, never()).createAccessToken(anyLong());
        assertEquals("refresh-token", user.getRefreshToken());
    }

    @Test
    @DisplayName("로그아웃한 회원은 refresh token이 없어 재발급받을 수 없다")
    void reissueAfterLogout() {

        // given
        user.updateRefreshToken(null);
        givenValidRefreshToken("refresh-token");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when & then
        assertThrows(
            ReissueFailException.class,
            () -> authService.reissue("refresh-token")
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원의 refresh token으로는 재발급받을 수 없다")
    void reissueUserNotFound() {

        // given
        givenValidRefreshToken("refresh-token");
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> authService.reissue("refresh-token")
        );
    }

    @Test
    @DisplayName("로그아웃하면 저장된 refresh token을 삭제한다")
    void logout() {

        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        authService.logout(1L, "refresh-token");

        // then
        assertNull(user.getRefreshToken());
    }

    @Test
    @DisplayName("저장된 refresh token과 다른 토큰으로 로그아웃하면 refresh token을 유지한다")
    void logoutTokenMismatch() {

        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        authService.logout(1L, "other-device-token");

        // then
        assertEquals("refresh-token", user.getRefreshToken());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 로그아웃할 수 없다")
    void logoutUserNotFound() {

        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> authService.logout(99L, "refresh-token")
        );
    }

    private void givenValidRefreshToken(String refreshToken) {
        when(jwtTokenProvider.validateRefreshToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getRefreshUserId(refreshToken)).thenReturn(1L);
    }
}
