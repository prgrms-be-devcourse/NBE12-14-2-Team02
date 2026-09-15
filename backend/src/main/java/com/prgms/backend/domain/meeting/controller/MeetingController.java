package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.response.MeetingResponse;
import com.prgms.backend.domain.meeting.service.MeetingService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController {
    private final MeetingService meetingService;

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
}
