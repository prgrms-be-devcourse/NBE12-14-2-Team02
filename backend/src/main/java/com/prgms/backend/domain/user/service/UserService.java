package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.dto.response.ProfileResponse;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.global.exception.custom.user.DuplicateEmailNickname;
import com.prgms.backend.global.exception.custom.user.PasswordMismatchException;
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final MeetingRepository meetingRepository;

    // 프로필 조회
    public ProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        return new ProfileResponse(user.getEmail(), user.getNickname());
    }

    // 닉네임 수정
    @Transactional
    public ProfileResponse updateNickname(Long userId, String nickname) {
        // 새로운 닉네임이 사용 중인지 확인
        if (authService.checkNickname(nickname)) {
            throw new DuplicateEmailNickname();
        }

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        user.updateNickname(nickname);
        return new ProfileResponse(user.getEmail(), user.getNickname());
    }

    // 비밀번호 수정
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

    /*
    // 회원 탈퇴
    public String withdraw(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        // 가지고 있는 모임 확인
        List<Meeting> meetings = meetingRepository.findByHostId(userId);

    }
     */


}
