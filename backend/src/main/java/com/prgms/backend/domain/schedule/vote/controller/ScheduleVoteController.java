package com.prgms.backend.domain.schedule.vote.controller;

import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteResponse;
import com.prgms.backend.domain.schedule.vote.service.ScheduleVoteService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/meetings/{meetingId}/schedule-poll/candidates/{candidateId}/votes/{meetingMemberId}")
@RequiredArgsConstructor
public class ScheduleVoteController {
    private final ScheduleVoteService scheduleVoteService;

    @PutMapping
    public ResponseEntity<ApiResponse<ScheduleVoteResponse.Saved>> upsert(
            @PathVariable Long meetingId,
            @PathVariable Long candidateId,
            @PathVariable Long meetingMemberId,
            @Valid @RequestBody ScheduleVoteRequest.Submit request
            ){

        ScheduleVoteResponse.Saved response = scheduleVoteService.submit(
                meetingId,
                candidateId,
                meetingMemberId,
                request
        );



        return ResponseEntity.ok(
                ApiResponse.success(200,response)
        );
    }

}
