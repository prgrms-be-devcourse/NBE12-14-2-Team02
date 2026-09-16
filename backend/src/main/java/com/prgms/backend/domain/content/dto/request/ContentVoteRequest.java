package com.prgms.backend.domain.content.dto.request;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import jakarta.validation.constraints.NotNull;

public record ContentVoteRequest(
        @NotNull(message = "후보 ID는 필수입니다.")
        Long candidateId,
        @NotNull(message = "선호도는 필수입니다.")
        ContentPreference preference
) {
}
