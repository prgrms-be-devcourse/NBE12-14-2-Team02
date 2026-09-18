package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.dto.ProfileResponse;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.exception.DuplicateEmailNickname;
import com.prgms.backend.domain.user.exception.PasswordMismatchException;
import com.prgms.backend.domain.user.exception.UserNotFoundException;
import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        return new ProfileResponse(user.getEmail(), user.getNickname());
    }

    @Transactional
    public ProfileResponse updateNickname(Long userId, String nickname) {
        if (authService.checkNickname(nickname)) {
            throw new DuplicateEmailNickname();
        }

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        user.updateNickname(nickname);
        return new ProfileResponse(user.getEmail(), user.getNickname());
    }

    @Transactional
    public void updatePassword(Long userId, String password, String newPassword) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        // 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new PasswordMismatchException();
        }

        // 암호화한 비밀번호로 업데이트
        user.updatePassword(passwordEncoder.encode(newPassword));
    }

    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
}
