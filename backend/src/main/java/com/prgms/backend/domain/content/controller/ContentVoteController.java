package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.request.ContentVoteRequest;
import com.prgms.backend.domain.content.dto.response.ContentVoteResponse;
import com.prgms.backend.domain.content.service.ContentVoteService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-votes")
public class ContentVoteController {
    private final ContentVoteService contentVoteService;

    @PutMapping
    public ResponseEntity<ApiResponse<ContentVoteResponse>> upsert(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid@RequestBody ContentVoteRequest request
    ){
        Long userId = securityUser.getId();
        ContentVoteResponse response = contentVoteService.upsert(meetingId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
}
