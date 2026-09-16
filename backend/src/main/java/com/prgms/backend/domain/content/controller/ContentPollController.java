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
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-poll")
public class ContentPollController {
    private final ContentPollService contentPollService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentPollResponse>> create(
            @PathVariable Long meetingId,
            @Valid @RequestBody ContentPollCreateRequest request
            ){
        ContentPollResponse response = contentPollService.create(meetingId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201,response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ContentPollDetailResponse>> get(
            @PathVariable Long meetingId,
            @RequestParam Long meetingMemberId
    ){
        ContentPollDetailResponse response = contentPollService.get(meetingId, meetingMemberId);
        return ResponseEntity.ok(ApiResponse.success(200,response));
    }

}
