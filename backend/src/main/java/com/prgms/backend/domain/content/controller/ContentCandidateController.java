package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.ContentCandidateCreateRequest;
import com.prgms.backend.domain.content.dto.ContentCandidateResponse;
import com.prgms.backend.domain.content.dto.ContentCandidateUpdateRequest;
import com.prgms.backend.domain.content.service.ContentCandidateService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/content-poll/candidates")
public class ContentCandidateController {
    private final ContentCandidateService contentCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentCandidateResponse>> create(
            @PathVariable Long meetingId,
            @RequestParam Long meetingMemberId,
            @Valid @RequestBody ContentCandidateCreateRequest request
            ){
        ContentCandidateResponse response =
                contentCandidateService.create(meetingId, meetingMemberId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }

    @PatchMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<ContentCandidateResponse>> update(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @RequestParam Long meetingMemberId,
            @Valid @RequestBody ContentCandidateUpdateRequest request
    ){
        ContentCandidateResponse response =
                contentCandidateService.update(meetingId, candidateId, meetingMemberId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @RequestParam Long meetingMemberId
    ){
        contentCandidateService.delete(meetingId, candidateId, meetingMemberId);
        return ResponseEntity.noContent().build();
    }
}
