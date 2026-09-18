package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.dto.request.ContentPollConfirmRequest;
import com.prgms.backend.domain.content.dto.request.ContentPollCreateRequest;
import com.prgms.backend.domain.content.dto.request.ContentPollDeadlineUpdateRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollDetailResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResultsResponse;
import com.prgms.backend.domain.content.service.ContentPollService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-poll")
public class ContentPollController {
    private final ContentPollService contentPollService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentPollResponse>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollCreateRequest request
            ){
        Long userId = securityUser.getId();
        ContentPollResponse response = contentPollService.create(meetingId,userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201,response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentPollDetailResponse>> get(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getId();
        ContentPollDetailResponse response = contentPollService.get(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ContentPollResponse>> updateDeadline(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollDeadlineUpdateRequest request
    ){
        Long userId = securityUser.getId();
        ContentPollResponse response = contentPollService.updateDeadline(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<ContentPollResultsResponse>> getResults(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(defaultValue = "SCORE")ContentPollResultSort sort
            ){
        Long userId = securityUser.getId();
        ContentPollResultsResponse response = contentPollService.getResults(meetingId, userId, sort);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ContentPollResponse>> confirm(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollConfirmRequest request
    ) {
        Long userId = securityUser.getId();
        ContentPollResponse response =
                contentPollService.confirm(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

}
