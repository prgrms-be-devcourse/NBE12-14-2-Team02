package com.prgms.backend.domain.settlement.dto

import com.prgms.backend.domain.settlement.entity.SettlementAccount

@JvmRecord
data class AccountResponse(val memberId: Long, val bankName: String, val accountNumber: String, val accountHolder: String) {
    companion object {
        @JvmStatic fun from(account: SettlementAccount) = AccountResponse(
            account.memberId, account.bankName, account.accountNumber, account.accountHolder)
    }
}
