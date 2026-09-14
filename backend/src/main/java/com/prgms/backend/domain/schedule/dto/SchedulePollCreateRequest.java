package com.prgms.backend.domain.schedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record SchedulePollCreateRequest(
        @NotNull(message = "마감 시간은 필수입니다.")
        @Future(message = "마감 시간이 잘못 입력되었습니다.")
        LocalDateTime deadline,

        @NotEmpty(message = "일정 후보는 한개 이상 등록해야합니다.")
        List<@NotNull(message = "일정 후보 날짜는 필수입니다.") LocalDate> candidateDates

) {
}
