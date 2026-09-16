package com.prgms.backend.domain.schedule.candidate.controller;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.service.ScheduleCandidateService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/{meetingId}/schedule-poll/candidates")
@RequiredArgsConstructor
public class ScheduleCandidateController {
    private final ScheduleCandidateService scheduleCandidateService;

    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleCandidateResponse.Summary>> create(
            @PathVariable Long meetingId,
            @Valid @RequestBody ScheduleCandidateRequest.Create request

            ){
        ScheduleCandidateResponse.Summary response = scheduleCandidateService.create(meetingId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.success(
                                201,
                                response
                        )
                );

    }


}
