package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.request.ContentPollCreateRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollDetailResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResponse;
import com.prgms.backend.domain.content.service.ContentPollService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-poll")
public class ContentPollController {
    private final ContentPollService contentPollService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentPollResponse>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ContentPollCreateRequest request
            ){
        Long userId = Long.parseLong(jwt.getSubject());
        ContentPollResponse response = contentPollService.create(meetingId,userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201,response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentPollDetailResponse>> get(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.parseLong(jwt.getSubject());
        ContentPollDetailResponse response = contentPollService.get(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

}
