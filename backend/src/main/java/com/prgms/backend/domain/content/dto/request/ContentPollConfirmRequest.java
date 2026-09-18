package com.prgms.backend.domain.content.dto.request;

import jakarta.validation.constraints.NotNull;
public record ContentPollConfirmRequest(
        @NotNull(message = "후보 ID는 필수입니다.")
        Long candidateId
) {
}