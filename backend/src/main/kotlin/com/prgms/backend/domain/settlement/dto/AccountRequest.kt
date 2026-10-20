package com.prgms.backend.domain.settlement.dto

import jakarta.validation.constraints.*

@JvmRecord
data class AccountRequest(
    @field:NotBlank @field:Size(max = 100) val bankName: String?,
    @field:NotBlank @field:Size(max = 50) @field:Pattern(regexp = "[0-9]+(-[0-9]+)*") val accountNumber: String?,
    @field:NotBlank @field:Size(max = 100) val accountHolder: String?
)
