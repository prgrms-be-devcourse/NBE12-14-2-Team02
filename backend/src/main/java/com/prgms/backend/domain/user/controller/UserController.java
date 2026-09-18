package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.ProfileResponse;
import com.prgms.backend.domain.user.dto.SignUpRequest;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;
    private final UserService userService;

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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
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

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
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
        SignUpResponse response = new SignUpResponse(userId);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }

    @Operation(
            summary = "내 프로필 조회",
            description = "로그인한 사용자의 이메일과 닉네임을 조회합니다."
    )
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @AuthenticationPrincipal SecurityUser securityUser
            ) {
        Long userId = securityUser.getId();

        ProfileResponse response = userService.getProfile(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200,response));
    }

    public record UpdateNicknameRequest(
            String newNickname
    ) {
    }

    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateNickname(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody UpdateNicknameRequest request
    ) {
        Long userId = securityUser.getId();
        ProfileResponse response = userService.updateNickname(userId, request.newNickname());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200,response));
    }
}
