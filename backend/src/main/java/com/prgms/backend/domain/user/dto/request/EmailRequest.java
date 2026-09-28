package com.prgms.backend.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EmailRequest (
        @Email @NotBlank String email
) {
}
