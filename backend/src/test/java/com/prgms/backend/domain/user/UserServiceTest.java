package com.prgms.backend.domain.user;

import com.prgms.backend.domain.user.dto.response.ProfileResponse;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.global.exception.custom.user.DuplicateEmailNickname;
import com.prgms.backend.global.exception.custom.user.PasswordMismatchException;
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        authService = mock(AuthService.class);

        userService = new UserService(
            userRepository,
            passwordEncoder,
            authService
        );

        user = new User("user@test.com", "회원", "encoded-password");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("프로필을 조회하면 이메일과 닉네임을 반환한다")
    void getProfile() {

        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        ProfileResponse response = userService.getProfile(1L);

        // then
        assertEquals("user@test.com", response.email());
        assertEquals("회원", response.nickname());
    }

    @Test
    @DisplayName("존재하지 않는 회원의 프로필은 조회할 수 없다")
    void getProfileUserNotFound() {

        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> userService.getProfile(99L)
        );
    }

    @Test
    @DisplayName("사용 중이지 않은 닉네임으로 변경하면 변경된 프로필을 반환한다")
    void updateNickname() {

        // given
        when(authService.checkNickname("새닉네임")).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        ProfileResponse response = userService.updateNickname(1L, "새닉네임");

        // then
        assertEquals("새닉네임", user.getNickname());
        assertEquals("user@test.com", response.email());
        assertEquals("새닉네임", response.nickname());
    }

    @Test
    @DisplayName("이미 사용 중인 닉네임으로는 변경할 수 없다")
    void updateNicknameDuplicate() {

        // given
        when(authService.checkNickname("중복닉네임")).thenReturn(true);

        // when & then
        assertThrows(
            DuplicateEmailNickname.class,
            () -> userService.updateNickname(1L, "중복닉네임")
        );

        verify(userRepository, never()).findById(anyLong());
        assertEquals("회원", user.getNickname());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 닉네임을 변경할 수 없다")
    void updateNicknameUserNotFound() {

        // given
        when(authService.checkNickname("새닉네임")).thenReturn(false);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> userService.updateNickname(99L, "새닉네임")
        );
    }

    @Test
    @DisplayName("현재 비밀번호가 일치하면 새 비밀번호를 암호화해 변경한다")
    void updatePassword() {

        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(passwordEncoder.encode("NewPassw0rd!")).thenReturn("encoded-NewPassw0rd!");

        // when
        userService.updatePassword(1L, "password", "NewPassw0rd!");

        // then
        assertEquals("encoded-NewPassw0rd!", user.getPassword());
    }

    @Test
    @DisplayName("현재 비밀번호가 틀리면 비밀번호를 변경할 수 없다")
    void updatePasswordMismatch() {

        // given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // when & then
        assertThrows(
            PasswordMismatchException.class,
            () -> userService.updatePassword(1L, "wrong", "NewPassw0rd!")
        );

        verify(passwordEncoder, never()).encode(anyString());
        assertEquals("encoded-password", user.getPassword());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 비밀번호를 변경할 수 없다")
    void updatePasswordUserNotFound() {

        // given
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> userService.updatePassword(99L, "password", "NewPassw0rd!")
        );
    }
}
