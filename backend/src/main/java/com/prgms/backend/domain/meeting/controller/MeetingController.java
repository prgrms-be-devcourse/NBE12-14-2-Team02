package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.request.MeetingUpdateRequest;
import com.prgms.backend.domain.meeting.dto.response.MeetingResponse;
import com.prgms.backend.domain.meeting.service.MeetingService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController {
    private final MeetingService meetingService;

    // 모임 생성
    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponse>> createMeeting(
        @RequestParam Long hostId,
        @Valid @RequestBody MeetingCreateRequest request
    ){
        // 로그인 기능 연결되면 hostId 전달 -> 로그인 사용자에서 userId 추출하는 방식으로 변경 예정
        MeetingResponse response =
            meetingService.createMeeting(hostId, request);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(201, response));
    }

    // 모임 상세 조회
    @GetMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> getMeeting(
        @PathVariable Long meetingId
    ){
        MeetingResponse response = meetingService.getMeeting(meetingId);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    // 모임 목록 조회
    @GetMapping
    public ResponseEntity<ApiResponse<List<MeetingResponse>>> getMyMeetings(
        @RequestParam Long userId
    ){
        List<MeetingResponse> response = meetingService.getMyMeetings(userId);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }

    // 모임 수정
    @PutMapping("/{meetingId}")
    public ResponseEntity<ApiResponse<MeetingResponse>> updateMeeting(
        @PathVariable Long meetingId,
        @RequestParam Long hostId,
        @Valid @RequestBody MeetingUpdateRequest request
    ) {
        MeetingResponse response =
            meetingService.updateMeeting(
                meetingId,
                hostId,
                request
            );

        return ResponseEntity.ok(
            ApiResponse.success(200, response)
        );
    }
}
