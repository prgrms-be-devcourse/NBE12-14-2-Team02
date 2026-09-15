package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    final UserRepository userRepository;

    public Boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

}
