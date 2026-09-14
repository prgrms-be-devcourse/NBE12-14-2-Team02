package com.prgms.backend.domain.schedule.controller;

import com.prgms.backend.domain.schedule.dto.SchedulePollCreateRequest;
import com.prgms.backend.domain.schedule.dto.SchedulePollResponse;
import com.prgms.backend.domain.schedule.service.SchedulePollService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/schedule-poll")
public class SchedulePollController {
    private final SchedulePollService schedulePollService;

    @PostMapping
    public ResponseEntity<ApiResponse<SchedulePollResponse>> create(
            @PathVariable Long meetingId,
            @Valid @RequestBody SchedulePollCreateRequest request
            ){
        SchedulePollResponse response = schedulePollService.create(meetingId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        201,
                        response
                ));
    }

}
