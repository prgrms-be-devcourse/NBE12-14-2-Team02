package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.SignInRequest;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.swing.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    public record EmailCheckResponse(
            Boolean available
    ) {
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailCheckResponse>> checkEmail(
            @RequestParam String email
    ) {
        boolean exists = userService.checkEmail(email);

        EmailCheckResponse response = new EmailCheckResponse(!exists);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    public record NicknameCheckResponse(
            Boolean available
    ) {
    }

    public ResponseEntity<ApiResponse<NicknameCheckResponse>> checkNickname (
        @RequestParam String nickname
    ) {
        boolean exists = userService.checkNickname(nickname);

        NicknameCheckResponse response = new NicknameCheckResponse(!exists);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    public record signInResponse(
            Long userId
    ) {
    }

    public ResponseEntity<ApiResponse<?>> signIn (
            @Valid @RequestBody SignInRequest signInRequest
    ) {
        Long userId = userService.signIn(signInRequest.nickname(), signInRequest.email(), signInRequest.password());

        return ResponseEntity.created(ApiResponse);
    }

}
