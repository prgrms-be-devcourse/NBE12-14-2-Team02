package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.exception.DuplicateEmailNickname;
import com.prgms.backend.domain.user.exception.PasswordMissmatchException;
import com.prgms.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    public Boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    public Boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    public  void validateConfirmPassword(String confirmPassword, String password) {
        if (!password.matches(confirmPassword)) {
            throw new PasswordMissmatchException();
        }
    }

    public Long signup(String email, String nickname, String password, String confirmPassword) {

        validateConfirmPassword(password, confirmPassword);

        if (checkEmail(email) || checkNickname(nickname)) {
            throw new DuplicateEmailNickname();
        }
        User user = new User(email, nickname, passwordEncoder.encode(password));
        userRepository.save(user);
        return user.getId();
    }

}
