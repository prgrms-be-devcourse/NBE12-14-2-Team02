package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationDetailResponse;
import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationResponse;
import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.service.MeetingInvitationService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import io.jsonwebtoken.Jwt;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        MeetingInvitationResponse response =
            meetingInvitationService.createInvitation(
                meetingId,
                userId
            );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(201, response));
    }

    // 초대를 통한 모임 참여
    @PostMapping("/invitations/{inviteCode}/join")
    public ResponseEntity<ApiResponse<MeetingMemberResponse>> joinMeeting(
        @PathVariable String inviteCode,
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        MeetingMemberResponse response =
            meetingInvitationService.joinMeeting(
                inviteCode,
                userId
            );

        return ResponseEntity.ok(
            ApiResponse.success(200, response)
        );
    }

    // 초대 코드 목록 조회
    @GetMapping("/meetings/{meetingId}/invitations")
    public ResponseEntity<ApiResponse<List<MeetingInvitationResponse>>> getInvitations(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        List<MeetingInvitationResponse> response =
            meetingInvitationService.getInvitations(meetingId, userId);

        return ResponseEntity.ok(
            ApiResponse.success(200, response)
        );
    }

    // 초대받은 모임 정보 조회
    @GetMapping("/invitations/{inviteCode}")
    public ResponseEntity<ApiResponse<MeetingInvitationDetailResponse>> getInvitation(
        @PathVariable String inviteCode,
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        MeetingInvitationDetailResponse response =
            meetingInvitationService.getInvitation(inviteCode, userId);

        return ResponseEntity.ok(
            ApiResponse.success(200, response)
        );
    }

    // 초대 코드 삭제
    @DeleteMapping("/meetings/{meetingId}/invitations/{invitationId}")
    public ResponseEntity<ApiResponse<Void>> deleteInvitation(
        @PathVariable Long meetingId,
        @PathVariable Long invitationId,
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        meetingInvitationService.deleteInvitation(
            meetingId,
            invitationId,
            userId
        );

        return ResponseEntity.ok(
            ApiResponse.success(204, null)
        );
    }
}
