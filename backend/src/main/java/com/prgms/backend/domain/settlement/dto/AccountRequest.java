package com.prgms.backend.domain.settlement.dto;

import jakarta.validation.constraints.*;

public record AccountRequest(
        @NotBlank @Size(max = 100) String bankName,
        @NotBlank @Size(max = 50) @Pattern(regexp = "[0-9]+(-[0-9]+)*") String accountNumber,
        @NotBlank @Size(max = 100) String accountHolder) {}
