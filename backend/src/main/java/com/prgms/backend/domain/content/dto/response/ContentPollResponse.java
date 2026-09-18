package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;

import java.time.LocalDateTime;

public record ContentPollResponse(
        Long id,
        Long meetingId,
        LocalDateTime deadline,
        ContentPollStatus status,
        Long confirmedCandidateId
) {
    public static ContentPollResponse from(ContentPoll contentPoll) {
        return new ContentPollResponse(
                contentPoll.getId(),
                contentPoll.getMeetingId(),
                contentPoll.getDeadline(),
                contentPoll.getStatus(),
                contentPoll.getConfirmedCandidateId()
        );
    }
}