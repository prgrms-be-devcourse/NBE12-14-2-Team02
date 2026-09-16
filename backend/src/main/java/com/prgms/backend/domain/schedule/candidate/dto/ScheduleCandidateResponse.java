package com.prgms.backend.domain.schedule.candidate.dto;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;

import java.time.LocalDateTime;


public record ScheduleCandidateResponse(
        Long id,
        LocalDateTime candidateDate
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
