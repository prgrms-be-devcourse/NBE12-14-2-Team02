package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.request.NicknameRequest;
import com.prgms.backend.domain.user.dto.response.EmailCheckResponse;
import com.prgms.backend.domain.user.dto.response.NicknameCheckResponse;
import com.prgms.backend.domain.user.dto.response.ProfileResponse;
import com.prgms.backend.domain.user.dto.request.SignUpRequest;
import com.prgms.backend.domain.user.dto.request.UpdatePasswordRequest;
import com.prgms.backend.domain.user.dto.response.SignUpResponse;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.AuthService;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.domain.user.service.UserWithdrawService;
import com.prgms.backend.global.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
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
    private final UserWithdrawService userWithdrawService;

    @Operation(
            summary = "이메일 중복 확인",
            description = "사용자가 입력한 이메일이 이용중인지 중복을 확인합니다. "
    )
    @GetMapping("/check-email")
    @SecurityRequirements
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @RequestParam @Email @NotNull String email
    ) {
        boolean exists = authService.checkEmail(email);

        EmailCheckResponse response = new EmailCheckResponse(!exists);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
    }


    @Operation(
            summary = "닉네임 중복 확인",
            description = "사용자가 입력한 닉네임이 이용중인지 중복을 확인합니다. "
    )
    @SecurityRequirements
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname (
            @RequestParam @NotNull String nickname
    ) {
        boolean exists = authService.checkNickname(nickname);

        NicknameCheckResponse response = new NicknameCheckResponse(!exists);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
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


    @Operation(
            summary = "닉네임 변경",
            description = "로그인한 사용자의 닉네임을 새 닉네임으로 변경합니다."
    )
    @PatchMapping("/me/nickname")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateNickname(
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody @Valid NicknameRequest request
    ) {
        Long userId = securityUser.getId();
        ProfileResponse response = userService.updateNickname(userId, request.nickname());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(200,response));
    }


    @Operation(
            summary = "비밀번호 변경",
            description = "현재 비밀번호를 확인한 뒤, 새 비밀번호(회원가입과 동일한 형식 규칙 적용)로 변경합니다."
    )
    @PatchMapping("/me/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @Valid @RequestBody UpdatePasswordRequest request,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getId();
        userService.updatePassword(userId, request.password(), request.newPassword());

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.noContentSuccess("비밀번호를 변경했습니다."));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getId();
        userWithdrawService.withdraw(userId);

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.noContentSuccess("회원 탈퇴가 완료되었습니다."));
    }
}
