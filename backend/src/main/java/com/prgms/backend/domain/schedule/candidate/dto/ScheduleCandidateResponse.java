package com.prgms.backend.domain.schedule.candidate.dto;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleCandidateResponse{
    public record Summary(
            Long id,
            LocalDate candidateDate
    ) {
        public static Summary from(ScheduleCandidate candidate){
            return new Summary(
                    candidate.getId(),
                    candidate.getCandidateDate()
            );
        }
    }

}