package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.request.ContentVoteRequest;
import com.prgms.backend.domain.content.dto.response.ContentVoteResponse;
import com.prgms.backend.domain.content.service.ContentVoteService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-votes")
public class ContentVoteController {
    private final ContentVoteService contentVoteService;

    @PutMapping
    public ResponseEntity<ApiResponse<ContentVoteResponse>> upsert(
            @PathVariable Long meetingId,
            @RequestParam Long meetingMemberId,
            @Valid @RequestBody ContentVoteRequest request
    ){
        ContentVoteResponse response = contentVoteService.upsert(meetingId, meetingMemberId, request);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
}
