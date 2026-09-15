package com.prgms.backend.domain.schedule.dto;


import com.prgms.backend.domain.schedule.entity.SchedulePoll;
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
    //DTO를 반환하는 팩토리 메서드
    public static SchedulePollResponse from(SchedulePoll schedulePoll) {
        List<ScheduleCandidateResponse> candidates =
                schedulePoll.getCandidates().stream()
                        .map(ScheduleCandidateResponse::from)
                        .toList();
        return new SchedulePollResponse(
                schedulePoll.getId(),
                schedulePoll.getMeeting().getId(),
                schedulePoll.getDeadline(),
                schedulePoll.getStatus(),
                candidates
        );
    }
}
