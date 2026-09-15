package com.prgms.backend.domain.content.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record ContentPollCreateRequest(
        @NotNull(message = "마감 시간을 입력해주세요.")
        @Future(message = "마감 시간이 잘못 입력되었습니다.")
        LocalDateTime deadline
) {
}