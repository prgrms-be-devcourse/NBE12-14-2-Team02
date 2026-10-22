package com.prgms.backend.domain.settlement.dto

import com.prgms.backend.domain.settlement.entity.Settlement
import com.prgms.backend.domain.settlement.entity.SettlementStatus
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Balance
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Transfer
import java.time.LocalDateTime

@JvmRecord
data class SettlementResponse(
    val settlementId: Long?, val meetingId: Long, val status: SettlementStatus,
    val closedByMemberId: Long?, val closedAt: LocalDateTime?,
    val balances: List<Balance>, val transfers: List<Transfer>, val accounts: List<AccountResponse>,
    val missingAccountMemberIds: List<Long>
) {
    companion object {
        @JvmStatic fun forMember(settlement: Settlement, memberId: Long, personalOnly: Boolean): SettlementResponse {
            val balances = settlement.balances.map { Balance(it.memberId, it.paidAmount, it.shareAmount) }
                .filter { !personalOnly || it.memberId == memberId }
            val transfers = settlement.transfers.map { Transfer(it.senderId, it.recipientId, it.amount) }
                .filter { !personalOnly || it.senderId == memberId || it.recipientId == memberId }
            return SettlementResponse(
                settlement.id, settlement.meetingId, settlement.status, settlement.closedByMemberId, settlement.closedAt,
                balances, transfers,
                settlement.accounts.filter { account ->
                    transfers.any { it.senderId == memberId && it.recipientId == account.memberId }
                }.map { AccountResponse(it.memberId, it.bankName, it.accountNumber, it.accountHolder) },
                transfers.map { it.recipientId }.distinct().sorted()
                    .filter { id -> settlement.accounts.none { it.memberId == id } }
            )
        }
    }
}
