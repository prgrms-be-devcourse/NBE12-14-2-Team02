package com.prgms.backend.domain.meeting.dto.response;

import com.prgms.backend.domain.meeting.entity.MeetingMember;

import java.time.LocalDateTime;

public record MeetingMemberResponse(
    Long id,
    Long meetingId,
    Long userId,
    String nickname,
    LocalDateTime joinedAt,
    LocalDateTime leftAt
) {

    public static MeetingMemberResponse from(MeetingMember meetingMember) {
        return new MeetingMemberResponse(
            meetingMember.getId(),
            meetingMember.getMeeting().getId(),
            meetingMember.getUser().getId(),
            meetingMember.getUser().getNickname(),
            meetingMember.getJoinedAt(),
            meetingMember.getLeftAt()
        );
    }
}
