package com.prgms.backend.domain.schedule.vote.dto;

import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleVoteRequest {
    public record Submit(
            @NotNull(message = "선호도는 필수입니다.")
            SchedulePreference preference
    ) {
    }
}
