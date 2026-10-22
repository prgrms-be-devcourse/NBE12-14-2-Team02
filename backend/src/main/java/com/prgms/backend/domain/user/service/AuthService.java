package com.prgms.backend.domain.user.service;

import com.prgms.backend.domain.user.dto.request.LogInRequest;
import com.prgms.backend.domain.user.dto.TokenPair;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.global.exception.custom.user.*;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthService {

    // Flyway V1에서 정의한 users 테이블의 UNIQUE 제약 이름
    private static final String EMAIL_UNIQUE_CONSTRAINT = "UK6dotkott2kjsp8vw4d0m25fb7";
    private static final String NICKNAME_UNIQUE_CONSTRAINT = "UK2ty1xmrrgtn89xt7kyxx6ta7h";

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
    @Transactional
    public Long signup(String email, String nickname, String password, String confirmPassword) {

        validateConfirmPassword(password, confirmPassword);

        if (checkEmail(email) || checkNickname(nickname)) {
            throw new DuplicateEmailNickname();
        }
        User user = new User(email, nickname, passwordEncoder.encode(password));

        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException e) {
            if (isDuplicateEmailOrNickname(e)) {
                throw new DuplicateEmailNickname();
            }
            throw e;
        }
        return user.getId();
    }

    private boolean isDuplicateEmailOrNickname(DataIntegrityViolationException exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                String constraintName = violation.getConstraintName();
                // MySQL은 제약 이름 앞에 테이블 이름을 붙여 반환하기도 한다.
                return EMAIL_UNIQUE_CONSTRAINT.equals(constraintName)
                    || NICKNAME_UNIQUE_CONSTRAINT.equals(constraintName)
                    || ("users." + EMAIL_UNIQUE_CONSTRAINT).equals(constraintName)
                    || ("users." + NICKNAME_UNIQUE_CONSTRAINT).equals(constraintName);
            }
        }
        return false;
    }

    // 로그인
    @Transactional
    public TokenPair login(LogInRequest request) {

        User user = userRepository.findByEmail(request.email()).orElseThrow(()->
                new UserNotFoundException("가입되지 않은 이메일입니다."));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new LoginFailException();
        }

        // 비밀번호 확인 후 검사해야 탈퇴 여부가 외부에 노출되지 않음
        if (user.getStatus() == UserStatus.WITHDRAWN || user.getDeletedAt() != null) {
            throw new WithdrawUserException();
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        user.updateRefreshToken(refreshToken);

        return new TokenPair(accessToken, refreshToken);
    }

    // access token, refresh token 재발급
    @Transactional
    public TokenPair reissue(String refreshToken) {
        if (jwtTokenProvider.validateRefreshToken(refreshToken)) {
            Long userId = jwtTokenProvider.getRefreshUserId(refreshToken);

            User user = userRepository.findById(userId).orElseThrow(
                    () -> new UserNotFoundException("회원 정보를 찾을 수 없습니다.")
            );

            if (user.getStatus() == UserStatus.WITHDRAWN || user.getDeletedAt() != null) {
                throw new WithdrawUserException();
            }

            if(refreshToken.equals(user.getRefreshToken())) {

                String newAccessToken = jwtTokenProvider.createAccessToken(userId);
                String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);
                user.updateRefreshToken(newRefreshToken);

                return new TokenPair(newAccessToken, newRefreshToken);

            }
        }

        throw new ReissueFailException();
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

        if (Objects.equals(refreshToken, user.getRefreshToken())) {
            user.updateRefreshToken(null);
        }

    }
}
