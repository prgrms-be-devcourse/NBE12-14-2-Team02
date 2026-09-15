package com.prgms.backend.domain.schedule.dto;

import com.prgms.backend.domain.schedule.entity.ScheduleCandidate;

import java.time.LocalDate;


public record ScheduleCandidateResponse(
        Long id,
        LocalDate candidateDate
) {
    //DTO를 반환하는 팩토리 매서드
    public static ScheduleCandidateResponse from(
            ScheduleCandidate candidate
    ){
        return new ScheduleCandidateResponse(
                candidate.getId(),
                candidate.getCandidateDate()
        );
    }
}
