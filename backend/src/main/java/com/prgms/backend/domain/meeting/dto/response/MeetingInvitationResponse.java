package com.prgms.backend.domain.meeting.dto.response;

import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import java.time.LocalDateTime;

public record MeetingInvitationResponse(
    Long id,
    Long meetingId,
    String inviteCode,
    LocalDateTime expiresAt,
    LocalDateTime createAt
) {

    public static MeetingInvitationResponse from(MeetingInvitation invitation){
        return new MeetingInvitationResponse(
            invitation.getId(),
            invitation.getMeeting().getId(),
            invitation.getInviteCode(),
            invitation.getExpiresAt(),
            invitation.getCreatedAt()
        );
    }
}
