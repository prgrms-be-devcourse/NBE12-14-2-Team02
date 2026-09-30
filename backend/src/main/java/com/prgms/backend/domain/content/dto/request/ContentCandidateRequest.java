package com.prgms.backend.domain.content.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentCandidateRequest {

    public record Create(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
            String description
    ) {
    }

    public record Update(
            @NotBlank(message = "제목은 필수입니다.")
            @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
            String title,

            @Size(max = 500, message = "설명은 500자 이하여야 합니다.")
            String description
    ) {
    }
}
