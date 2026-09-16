package com.prgms.backend.domain.schedule.candidate.dto;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleCandidateResponse{
    //일정 투표 조회 시 후보 목록에 사용하는 DTO 클라이언트에서 candidateDate만 보여주면 될 듯.
    public record Summary(
            Long id,
            LocalDate candidateDate
    ) {
        public static ScheduleCandidateResponse.Summary from(ScheduleCandidate candidate){
            return new Summary(
                    candidate.getId(),
                    candidate.getCandidateDate()
            );
        }
    }

}