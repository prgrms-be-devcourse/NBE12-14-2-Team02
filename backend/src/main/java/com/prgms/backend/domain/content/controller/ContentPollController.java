package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.dto.request.ContentPollRequest;
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
    public ResponseEntity<ApiResponse<ContentPollResponse.Created>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollRequest.Create request
            ){
        Long userId = securityUser.getUserId();
        ContentPollResponse.Created response = contentPollService.create(meetingId,userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201,response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentPollResponse.Detail>> get(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getUserId();
        ContentPollResponse.Detail response = contentPollService.get(meetingId, userId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ContentPollResponse.DeadlineUpdate>> updateDeadline(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollRequest.UpdateDeadline request
    ){
        Long userId = securityUser.getUserId();
        ContentPollResponse.DeadlineUpdate response =
                contentPollService.updateDeadline(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @GetMapping("/results")
    public ResponseEntity<ApiResponse<ContentPollResultsResponse.Detail>> getResults(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @RequestParam(defaultValue = "SCORE")ContentPollResultSort sort
            ){
        Long userId = securityUser.getUserId();
        ContentPollResultsResponse.Detail response =
                contentPollService.getResults(meetingId, userId, sort);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<ContentPollResponse.Confirmed>> confirm(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentPollRequest.Confirm request
    ) {
        Long userId = securityUser.getUserId();
        ContentPollResponse.Confirmed response =
                contentPollService.confirm(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

}
