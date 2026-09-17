package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.dto.LogInRequest;
import com.prgms.backend.domain.user.dto.LogInResponse;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.exception.DuplicateEmailNickname;
import com.prgms.backend.domain.user.exception.LoginFailException;
import com.prgms.backend.domain.user.exception.PasswordMissmatchException;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;


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

    @Transactional(readOnly = true)
    public LogInResponse login(LogInRequest request) {

        User user = userRepository.findByEmail(request.email()).orElseThrow(()->
                new UsernameNotFoundException("존재하지 않는 사용자"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new LoginFailException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        return new LogInResponse(accessToken, refreshToken);
    }

}
