package com.prgms.backend.domain.content.dto.request;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentVoteRequest {

    public record Submit(
            @NotNull(message = "후보 ID는 필수입니다.")
            Long candidateId,
            @NotNull(message = "선호도는 필수입니다.")
            ContentPreference preference
    ) {
    }
}
