package com.prgms.backend.domain.schedule.poll.dto;

import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleParticipantResponse {

    public record MemberResponses(
            Long meetingMemberId,
            Long userId,
            String nickname,
            List<CandidateAnswer> answers
    ) {
    }

    //일정 후보 별 선호도를 주기 위한 dto
    public record CandidateAnswer(
            Long candidateId,
            LocalDate candidateDate,
            SchedulePreference preference
    ) {
    }
}