package com.prgms.backend.domain.settlement.dto

import com.prgms.backend.domain.settlement.service.SettlementCalculator

@JvmRecord
data class SettlementPreviewResponse(
    val balances: List<SettlementCalculator.Balance>, val transfers: List<SettlementCalculator.Transfer>,
    val missingAccounts: List<MissingAccount>
) {
    @JvmRecord data class MissingAccount(val memberId: Long, val nickname: String)
}
