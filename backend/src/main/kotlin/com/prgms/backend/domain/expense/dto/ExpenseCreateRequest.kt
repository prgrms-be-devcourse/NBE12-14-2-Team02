package com.prgms.backend.domain.expense.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.*
import java.math.BigDecimal

@JvmRecord
data class ExpenseCreateRequest(
    @field:NotBlank @field:Size(max = 255) val title: String?,
    @field:NotNull @field:DecimalMin("1") @field:Digits(integer = 12, fraction = 0) val amount: BigDecimal?,
    @field:Size(max = 2000) val memo: String?,
    @field:NotNull val splitMode: SplitMode?,
    val roundingUnit: Int?,
    @field:Positive val remainderMemberId: Long?,
    @field:NotEmpty @field:Size(max = 100) @field:Valid val participants: List<@NotNull Participant?>?,
    val randomRemainder: Boolean = false
) {
    constructor(title: String?, amount: BigDecimal?, memo: String?, splitMode: SplitMode?,
                roundingUnit: Int?, remainderMemberId: Long?, participants: List<Participant?>?) :
        this(title, amount, memo, splitMode, roundingUnit, remainderMemberId, participants, false)

    enum class SplitMode { EQUAL, EXACT }

    @JvmRecord
    data class Participant(
        @field:NotNull @field:Positive val memberId: Long?,
        @field:DecimalMin("0") @field:Digits(integer = 12, fraction = 0) val amount: BigDecimal?
    )
}
