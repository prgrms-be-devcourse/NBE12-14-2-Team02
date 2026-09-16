package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationResponse;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.service.MeetingInvitationService;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingInvitationController {

    private final MeetingInvitationService meetingInvitationService;

    // 초대 생성
    @PostMapping("/{meetingId}/invitations")
    public ResponseEntity<ApiResponse<MeetingInvitationResponse>> createInvitation(
        @PathVariable Long meetingId,
        @RequestParam Long hostId
    ){
        MeetingInvitationResponse response =
            meetingInvitationService.createInvitation(meetingId, hostId);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(201, response));
    }
}
