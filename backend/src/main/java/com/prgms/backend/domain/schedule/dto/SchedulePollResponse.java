package com.prgms.backend.domain.schedule.dto;


import com.prgms.backend.domain.schedule.entity.SchedulePollStatus;

import java.time.LocalDateTime;
import java.util.List;

public record SchedulePollResponse(
        Long id,
        Long meetingId,
        LocalDateTime deadline,
        SchedulePollStatus status,
        List<ScheduleCandidateResponse> candidates
) {
}
