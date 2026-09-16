package com.prgms.backend.domain.schedule.candidate.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleCandidateRequest {

    public record Create(

            @Future(message = "후보 날짜는 오늘보다 이후여야 합니다.")
            @NotNull(message = "날짜는 필수입니다")
            LocalDate candidateDate

    ) {
    }

    public record Update(
            @Future(message = "후보 날짜는 오늘보다 이후여야 합니다.")
            @NotNull(message = "날짜는 필수입니다")
            LocalDate candidateDate
    ){
    }

}