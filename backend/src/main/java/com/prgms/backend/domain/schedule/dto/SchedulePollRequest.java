package com.prgms.backend.domain.schedule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchedulePollRequest{



        //일정투표 생성용 DTO
        public record Create(
                @NotNull(message = "마감 시간은 필수입니다.")
                @Future(message = "마감 시간은 현재보다 이후여야 합니다.")
                LocalDateTime deadline,

                @NotEmpty(message = "일정 후보는 한개 이상 등록해야합니다.")
                @UniqueElements(
                        message = "동일한 일정 후보를 중복 등록할 수 없습니다."
                )
                List<
                        @NotNull(message = "일정 후보 날짜는 필수입니다.")
                        @FutureOrPresent(message = "지난 날짜는 후보로 등록할 수 없습니다.")
                                LocalDate
                        > candidateDates

        ) {}
        //마감시각 수정용 DTO
        public record UpdateDeadline(
                @NotNull(message = "마감 시간은 필수입니다.")
                @Future(message = "마감 시간은 현재보다 이후여야 합니다.")
                LocalDateTime deadline
        ){}

}

