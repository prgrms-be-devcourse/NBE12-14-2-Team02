package com.prgms.backend.domain.schedule.candidate.controller;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.service.ScheduleCandidateService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/{meetingId}/schedule-poll/candidates")
@RequiredArgsConstructor
public class ScheduleCandidateController {
    private final ScheduleCandidateService scheduleCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCandidateResponse.Summary>> create(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ScheduleCandidateRequest.Create request

            ){

        Long userId = securityUser.getUserId();


        ScheduleCandidateResponse.Summary response = scheduleCandidateService.create(meetingId, userId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                201,
                                response
                        )
                );

    }


    @PatchMapping("/{candidateId}")
    public ResponseEntity<ApiResponse<ScheduleCandidateResponse.Summary>> update(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal SecurityUser securityUser,
            @Valid @RequestBody ScheduleCandidateRequest.Update request
    ){

        Long userId = securityUser.getUserId();
        ScheduleCandidateResponse.Summary response = scheduleCandidateService.update(meetingId, candidateId, userId, request);



        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success(
                        200,
                        response
                ));
    }


    @DeleteMapping("/{candidateId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getUserId();

        scheduleCandidateService.delete(meetingId, candidateId,userId);


        return ResponseEntity.noContent().build();
    }


}
