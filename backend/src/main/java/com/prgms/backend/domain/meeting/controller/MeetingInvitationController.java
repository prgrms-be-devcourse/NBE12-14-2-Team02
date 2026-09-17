package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationResponse;
import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
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
@RequestMapping("/api")
public class MeetingInvitationController {

    private final MeetingInvitationService meetingInvitationService;

    // 초대 생성
    @PostMapping("/meetings/{meetingId}/invitations")
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

    // 초대를 통한 모임 참여
    @PostMapping("/invitations/{inviteCode}/join")
    public ResponseEntity<ApiResponse<MeetingMemberResponse>> joinMeeting(
        @PathVariable String inviteCode,
        @RequestParam Long userId
    ) {
        MeetingMemberResponse response =
            meetingInvitationService.joinMeeting(inviteCode, userId);

        return ResponseEntity.ok(ApiResponse.success(200, response));
    }
}
