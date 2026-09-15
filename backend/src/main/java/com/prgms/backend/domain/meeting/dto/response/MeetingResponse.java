package com.prgms.backend.domain.meeting.dto.response;

import com.prgms.backend.domain.meeting.entity.Meeting;

public record MeetingResponse(
    Long id,
    Long hostId,
    String name,
    String description
) {

    public static MeetingResponse from(Meeting meeting){
        return new MeetingResponse(
            meeting.getId(),
            meeting.getHost().getId(),
            meeting.getName(),
            meeting.getDescription()
        );
    }
}
