package com.prgms.backend.domain.meeting.dto.response;

import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import java.time.LocalDateTime;

public record MeetingInvitationDetailResponse(
    String inviteCode,
    Long meetingId,
    String meetingName,
    LocalDateTime expiresAt,
    boolean alreadyJoined
) {

    public static MeetingInvitationDetailResponse from(
        MeetingInvitation invitation,
        boolean alreadyJoined
    ) {
        return new MeetingInvitationDetailResponse(
            invitation.getInviteCode(),
            invitation.getMeeting().getId(),
            invitation.getMeeting().getName(),
            invitation.getExpiresAt(),
            alreadyJoined
        );
    }
}
