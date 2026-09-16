package com.prgms.backend.domain.schedule.poll.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchedulePollRequest{



        //일정투표 생성용 DTO
        public record Create(
                @NotNull(message = "마감 시간은 필수입니다.")
                @Future(message = "마감 시간은 현재보다 이후여야 합니다.")
                LocalDateTime deadline

        ) {}
        //마감시각 수정용 DTO
        public record UpdateDeadline(
                @NotNull(message = "마감 시간은 필수입니다.")
                @Future(message = "마감 시간은 현재보다 이후여야 합니다.")
                LocalDateTime deadline
        ){}

}

