package com.prgms.backend.domain.schedule.poll.event;

public record SchedulePollClosedEvent(
        Long pollId,
        Long meetingId
) {
}
