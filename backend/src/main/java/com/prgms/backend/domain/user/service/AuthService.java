package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.dto.LogInRequest;
import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.exception.DuplicateEmailNickname;
import com.prgms.backend.domain.user.exception.LoginFailException;
import com.prgms.backend.domain.user.exception.PasswordMismatchException;
import com.prgms.backend.domain.user.exception.UserNotFoundException;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;


    // 이메일 중복 체크
    public Boolean checkEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    // 닉네임 중복 체크
    public Boolean checkNickname(String nickname) {
        return userRepository.existsByNickname(nickname);
    }

    // 회원가입에서 비밀번호와 비밀번호 확인이 일치하는지 확인
    public  void validateConfirmPassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new PasswordMismatchException();
        }
    }

    // 회원가입
    public Long signup(String email, String nickname, String password, String confirmPassword) {

        validateConfirmPassword(password, confirmPassword);

        if (checkEmail(email) || checkNickname(nickname)) {
            throw new DuplicateEmailNickname();
        }
        User user = new User(email, nickname, passwordEncoder.encode(password));
        userRepository.save(user);
        return user.getId();
    }

    // 로그인
    @Transactional
    public TokenPair login(LogInRequest request) {

        User user = userRepository.findByEmail(request.email()).orElseThrow(()->
                new UsernameNotFoundException("존재하지 않는 사용자"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new LoginFailException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

    // refresh token 재발급
    public String reissue(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            Long userId = jwtTokenProvider.getUserId(refreshToken);

            User user = userRepository.findById(userId).orElseThrow(
                    () -> new UserNotFoundException("회원 정보를 찾을 수 없습니다.")
            );

            if(user.getRefreshToken().equals(refreshToken)) {
                return jwtTokenProvider.createAccessToken(userId);
            }

            return null;
        }

        return null;
    }

    // 로그아웃
    @Transactional
    public void logout(
            Long userId,
            String refreshToken
    ) {
        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보를 찾을 수 없습니다.")
        );

        if (user.getRefreshToken().equals(refreshToken)) {
            user.updateRefreshToken(null);
        }

    }
}
