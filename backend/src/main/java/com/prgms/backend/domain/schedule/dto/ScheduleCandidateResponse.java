package com.prgms.backend.domain.schedule.dto;

import java.time.LocalDate;

public record ScheduleCandidateResponse(
        Long id,
        LocalDate candidateDate
) {
}
