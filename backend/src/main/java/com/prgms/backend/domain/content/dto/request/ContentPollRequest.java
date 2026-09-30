package com.prgms.backend.domain.content.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPollRequest {

    public record Create(
            @NotNull(message = "마감 시간을 입력해주세요.")
            @Future(message = "마감 시간이 잘못 입력되었습니다.")
            LocalDateTime deadline
    ) {
    }

    public record UpdateDeadline(
            @NotNull(message = "마감시간을 입력해주세요.")
            @Future(message = "마감시간이 잘못입력되었습니다.")
            LocalDateTime deadline
    ) {
    }

    public record Confirm(
            @NotNull(message = "후보 ID는 필수입니다.")
            Long candidateId
    ) {
    }
}
