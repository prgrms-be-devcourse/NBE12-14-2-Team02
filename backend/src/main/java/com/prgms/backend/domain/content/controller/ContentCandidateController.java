package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.request.ContentCandidateCreateRequest;
import com.prgms.backend.domain.content.dto.request.ContentCandidateUpdateRequest;
import com.prgms.backend.domain.content.dto.response.ContentCandidateResponse;
import com.prgms.backend.domain.content.service.ContentCandidateService;
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
@RequestMapping("/api/meetings/{meetingId}/content-poll/candidates")
public class ContentCandidateController {
    private final ContentCandidateService contentCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentCandidateResponse>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ContentCandidateCreateRequest request
            ){
        Long userId = Long.parseLong(jwt.getSubject());
        ContentCandidateResponse response =
                contentCandidateService.create(meetingId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }

    @PatchMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<ContentCandidateResponse>> update(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ContentCandidateUpdateRequest request
    ){
        Long userId = Long.parseLong(jwt.getSubject());
        ContentCandidateResponse response =
                contentCandidateService.update(meetingId, candidateId, userId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = Long.parseLong(jwt.getSubject());
        contentCandidateService.delete(meetingId, candidateId, userId);
        return ResponseEntity.noContent().build();
    }
}
