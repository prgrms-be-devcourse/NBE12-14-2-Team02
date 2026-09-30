package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.service.MeetingMemberService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingMemberController {

    private final MeetingMemberService meetingMemberService;

    // 현재 모임원 목록 조회
    @GetMapping("/{meetingId}/members")
    public ResponseEntity<ApiResponse<List<MeetingMemberResponse>>> getMeetingMembers(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal SecurityUser securityUser
    ){
        Long userId = securityUser.getId();

        List<MeetingMemberResponse> responses =
            meetingMemberService.getMeetingMembers(meetingId, userId);

        return ResponseEntity.ok(ApiResponse.success(200, responses));
    }

    // 현재 로그인한 사용자가 모임 탈퇴
    @DeleteMapping("/{meetingId}/members/me")
    public ResponseEntity<ApiResponse<MeetingMemberResponse>> leaveMeeting(
        @PathVariable Long meetingId,
        @AuthenticationPrincipal SecurityUser securityUser
    ) {
        Long userId = securityUser.getId();

        MeetingMemberResponse response =
            meetingMemberService.leaveMeeting(meetingId, userId);

        return ResponseEntity.ok(
            ApiResponse.success(200, response)
        );
    }
}
