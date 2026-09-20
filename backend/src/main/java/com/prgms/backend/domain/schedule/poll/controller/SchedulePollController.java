package com.prgms.backend.domain.schedule.poll.controller;

import com.prgms.backend.domain.schedule.poll.dto.SchedulePollRequest;
import com.prgms.backend.domain.schedule.poll.dto.SchedulePollResponse;
import com.prgms.backend.domain.schedule.poll.dto.ScheduleResultResponse;
import com.prgms.backend.domain.schedule.poll.service.SchedulePollService;
import com.prgms.backend.domain.schedule.poll.service.ScheduleResultService;
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
@RequestMapping("/api/meetings/{meetingId}/schedule-poll")
public class SchedulePollController {
    private final SchedulePollService schedulePollService;
    private final ScheduleResultService scheduleResultService;

    @PostMapping
    public ResponseEntity<ApiResponse<SchedulePollResponse.Created>> create(
            @PathVariable Long meetingId,
            @Valid @RequestBody SchedulePollRequest.Create request,
            @AuthenticationPrincipal SecurityUser securityUser
            ){

        Long userId = securityUser.getUserId();

        SchedulePollResponse.Created response = schedulePollService.create(meetingId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        201,
                        response
                ));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<SchedulePollResponse.Detail>> get(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){

        Long userId = securityUser.getUserId();
        SchedulePollResponse.Detail response =
                schedulePollService.get(meetingId, userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        200,
                        response
                )
        );

    }

    @PatchMapping
    public ResponseEntity<ApiResponse<SchedulePollResponse.DeadlineUpdate>> updateDeadline(
            @PathVariable Long meetingId,
            @Valid @RequestBody SchedulePollRequest.UpdateDeadline request,
            @AuthenticationPrincipal SecurityUser securityUser
    ){

        Long userId = securityUser.getUserId();

        SchedulePollResponse.DeadlineUpdate response =
                schedulePollService.updateDeadline(meetingId, userId,request);

        return ResponseEntity.ok(
                ApiResponse.success(200,response)
        );

    }

    // 후보별 순위와 참여자별 응답을 결과 화면에 한 번에 반환한다.
    @GetMapping("/results")
    public ResponseEntity<ApiResponse<ScheduleResultResponse.Detail>> getResults(
            @PathVariable Long meetingId,
            @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getUserId();

        ScheduleResultResponse.Detail response =
                scheduleResultService.getResults(
                        meetingId,
                        userId
                );

        return ResponseEntity.ok(
                ApiResponse.success(200, response)
        );
    }

}
