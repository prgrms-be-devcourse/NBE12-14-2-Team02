package com.prgms.backend.domain.content.controller;

import com.prgms.backend.domain.content.dto.request.ContentCandidateRequest;
import com.prgms.backend.domain.content.dto.response.ContentCandidateResponse;
import com.prgms.backend.domain.content.service.ContentCandidateService;
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
@RequestMapping("/api/meetings/{meetingId}/content-poll/candidates")
public class ContentCandidateController {
    private final ContentCandidateService contentCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContentCandidateResponse.Saved>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentCandidateRequest.Create request
            ){
        Long userId = securityUser.getUserId();
        ContentCandidateResponse.Saved response =
                contentCandidateService.create(meetingId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, response));
    }

    @PatchMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<ContentCandidateResponse.Saved>> update(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ContentCandidateRequest.Update request
    ){
        Long userId = securityUser.getUserId();
        ContentCandidateResponse.Saved response =
                contentCandidateService.update(meetingId, candidateId, userId, request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(200, response));
    }

    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getUserId();
        contentCandidateService.delete(meetingId, candidateId, userId);
        return ResponseEntity.noContent().build();
    }
}
