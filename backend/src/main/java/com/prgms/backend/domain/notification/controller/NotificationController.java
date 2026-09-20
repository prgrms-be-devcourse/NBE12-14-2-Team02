package com.prgms.backend.domain.notification.controller;

import com.prgms.backend.domain.notification.dto.response.NotificationResponse;
import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMine(
            @AuthenticationPrincipal SecurityUser securityUser
            ){
        Long userId = securityUser.getUserId();
        List<NotificationResponse> response =
                notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getUserId();
        NotificationResponse response = notificationService.markRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }
}
