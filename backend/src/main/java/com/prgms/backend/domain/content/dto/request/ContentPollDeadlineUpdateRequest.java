package com.prgms.backend.domain.content.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ContentPollDeadlineUpdateRequest(
        @NotNull(message = "마감시간을 입력해주세요.")
        @Future(message = "마감시간이 잘못입력되었습니다.")
        LocalDateTime deadline
) {
}
