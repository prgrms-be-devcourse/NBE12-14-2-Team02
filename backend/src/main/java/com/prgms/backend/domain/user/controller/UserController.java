package com.prgms.backend.domain.user.controller;

import com.prgms.backend.domain.user.service.UserService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        EmailCheckResponse response =
                new EmailCheckResponse(!exists);

        return ResponseEntity.ok(
                ApiResponse.success(200, response)
        );
    }

}
