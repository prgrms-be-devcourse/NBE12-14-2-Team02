package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.dto.ProfileResponse;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<ProfileResponse>> getMeetings(
            @AuthenticationPrincipal SecurityUser securityUser
            ) {
        Long userId = securityUser.getId();

        ProfileResponse response = userService.getProfile(userId);

        return ResponseEntity.ok(ApiResponse.success(200,response));
    }
}
