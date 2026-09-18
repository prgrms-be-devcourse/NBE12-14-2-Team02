package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.*;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record EmailCheckResponse(
            Boolean available
    ) {
    }
    @Operation(
            summary = "이메일 중복 확인",
            description = "사용자가 입력한 이메일이 이용중인지 중복을 확인합니다. "
    )
    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @RequestParam String email
    ) {
        boolean exists = authService.checkEmail(email);

        EmailCheckResponse response = new EmailCheckResponse(!exists);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    public record NicknameCheckResponse(
            Boolean available
    ) {
    }

    @Operation(
            summary = "닉네임 중복 확인",
            description = "사용자가 입력한 닉네임이 이용중인지 중복을 확인합니다. "
    )
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname (
        @RequestParam String nickname
    ) {
        boolean exists = authService.checkNickname(nickname);

        NicknameCheckResponse response = new NicknameCheckResponse(!exists);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    public record SignUpResponse(
            Long userId
    ) {
    }

    @Operation(
            summary = "회원가입",
            description = "이메일, 닉네임, 비밀번호를 입력받아 회원가입을 진행합니다."
    )
    @SecurityRequirements
    @PostMapping("/sign-up")
    public ResponseEntity<ApiResponse<SignUpResponse>> signUp (
            @Valid @RequestBody SignUpRequest signUpRequest
    ) {
        Long userId = authService.signup(signUpRequest.email(), signUpRequest.nickname(), signUpRequest.password(), signUpRequest.confirmPassword());
        SignUpResponse signUpResponse = new SignUpResponse(userId);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(201, signUpResponse));
    }

    @Operation(
            summary = "로그인",
            description = "이메일과 비밀번호로 로그인하고, 액세스 토큰과 리프레시 토큰(쿠키)을 발급받습니다."
    )
    @SecurityRequirements
    @PostMapping("/log-in")
    public ResponseEntity<ApiResponse<LogInResponse>> login (
            @Valid @RequestBody LogInRequest logInRequest
    ) {
        TokenPair pair = authService.login(logInRequest);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", pair.refreshToken())
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(Duration.ofDays(14))
                .build();

        LogInResponse response = new LogInResponse(pair.accessToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(201, response));
    }

    @Operation(
            summary = "액세스 토큰 재발급",
            description = "쿠키로 전달된 리프레시 토큰을 검증하여 새로운 액세스 토큰을 발급합니다."
    )
    @SecurityRequirements
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueResponse>> reissue(
            @CookieValue(value = "refreshToken", required = false)
            String refreshToken
    ) {
        String newAccessToken = authService.reissue(refreshToken);

        return ResponseEntity.ok(ApiResponse.success(200,new ReissueResponse(newAccessToken)));
    }

}
