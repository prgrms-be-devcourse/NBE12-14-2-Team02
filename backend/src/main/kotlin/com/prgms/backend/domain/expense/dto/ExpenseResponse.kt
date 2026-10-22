package com.prgms.backend.domain.expense.dto

import com.prgms.backend.domain.expense.entity.Expense
import java.time.LocalDateTime

@JvmRecord
data class ExpenseResponse(
    val id: Long?, val meetingId: Long?, val payerMemberId: Long?, val title: String,
    val amount: Long, val memo: String?, val splitMode: String?, val roundingUnit: Int?,
    val remainderMemberId: Long?, val participants: List<Share>, val createdAt: LocalDateTime?, val hasReceipt: Boolean
) {
    @JvmRecord data class Share(val memberId: Long, val amount: Long)
    companion object {
        @JvmStatic fun from(expense: Expense) = ExpenseResponse(
            expense.id, expense.meetingId, expense.payerMemberId, expense.title, expense.amount, expense.memo,
            expense.splitMode, expense.roundingUnit, expense.remainderMemberId,
            expense.getParticipantAmounts().toSortedMap().map { (id, amount) -> Share(id, amount) },
            expense.createdAt, expense.receiptKey != null
        )
    }
}
