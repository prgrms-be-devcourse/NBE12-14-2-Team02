package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.dto.ProfileResponse;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.exception.UserNotFoundException;
import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
