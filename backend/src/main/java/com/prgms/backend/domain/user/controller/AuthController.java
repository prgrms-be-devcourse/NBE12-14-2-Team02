package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.*;
import com.prgms.backend.domain.user.entity.SecurityUser;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


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

        return ResponseEntity
                .status(HttpStatus.CREATED)
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
        ReissueResponse response = new ReissueResponse(newAccessToken);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
    }

    @Operation(
            summary = "로그아웃",
            description = "저장된 리프레시 토큰을 무효화하고 리프레시 토큰 쿠키를 삭제합니다."
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse> logout(
            @AuthenticationPrincipal SecurityUser securityUser,
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        Long userId = securityUser.getUserId();

        authService.logout(userId, refreshToken);

        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .httpOnly(true)
                .secure(true)
                .sameSite("Strict")
                .path("/api/auth")       // 로그인 때 설정한 path와 반드시 동일해야 지워짐
                .maxAge(0)        // 즉시 만료
                .build();


        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.noContentSuccess("로그아웃 되었습니다."));
    }

}
