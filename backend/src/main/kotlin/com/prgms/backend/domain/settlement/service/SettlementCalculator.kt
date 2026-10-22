package com.prgms.backend.domain.settlement.service

import com.prgms.backend.domain.expense.entity.Expense
import org.springframework.stereotype.Component
import java.util.Collections
import java.util.IdentityHashMap

@Component
class SettlementCalculator {
    @JvmRecord data class Balance(val memberId: Long, val paidAmount: Long, val shareAmount: Long) {
        fun netAmount(): Long = paidAmount - shareAmount
    }
    @JvmRecord data class Transfer(val senderId: Long, val recipientId: Long, val amount: Long)
    class Result(balances: List<Balance>, transfers: List<Transfer>) {
        val balances: List<Balance> = java.util.List.copyOf(balances)
        val transfers: List<Transfer> = java.util.List.copyOf(transfers)
        fun balances() = balances
        fun transfers() = transfers
    }

    fun calculate(meetingId: Long, expenses: List<Expense?>?, memberIds: Set<Long?>?): Result {
        require(meetingId > 0 && expenses != null && !memberIds.isNullOrEmpty() && memberIds.all { it != null && it > 0 }) {
            "모임과 참여자 정보가 필요합니다."
        }
        val ids = memberIds.filterNotNull().toSet()
        val paidAmounts = ids.associateWith { 0L }.toSortedMap()
        val shareAmounts = ids.associateWith { 0L }.toSortedMap()
        val seenExpenses = Collections.newSetFromMap(IdentityHashMap<Expense, Boolean>())
        val seenIds = hashSetOf<Long>()
        for (expense in expenses) {
            require(expense != null && seenExpenses.add(expense) && (expense.id == null || seenIds.add(requireNotNull(expense.id)))) {
                "지출이 비어 있거나 중복되었습니다."
            }
            validateExpense(meetingId, expense, ids)
            val payerId = expense.payerMemberId
            paidAmounts[payerId] = Math.addExact(paidAmounts.getValue(payerId), expense.amount)
            for ((memberId, amount) in getShares(expense)) {
                shareAmounts[memberId] = Math.addExact(shareAmounts.getValue(memberId), amount)
            }
        }
        val balances = paidAmounts.map { (id, amount) -> Balance(id, amount, shareAmounts.getValue(id)) }
        return Result(balances, createTransfers(balances))
    }

    private fun validateExpense(meetingId: Long, expense: Expense, memberIds: Set<Long>) {
        require(expense.meetingId == meetingId && expense.amount > 0 && expense.payerMemberId in memberIds &&
            expense.getParticipantIds().isNotEmpty() && memberIds.containsAll(expense.getParticipantIds())) {
            "모임 또는 지출 참여자 정보를 확인해주세요."
        }
    }

    private fun getShares(expense: Expense): Map<Long, Long> {
        val shares = expense.getParticipantAmounts()
        if (shares.isEmpty() && expense.splitMode == null) {
            // 개인별 부담액을 저장하기 전 지출에만 기존 균등 분배 규칙을 적용합니다.
            val participants = expense.getParticipantIds().sorted()
            val each = expense.amount / participants.size
            val remainder = expense.amount % participants.size
            return participants.mapIndexed { index, id -> id to each + if (index < remainder) 1L else 0L }.toMap()
        }
        require(shares.keys == expense.getParticipantIds()) { "지출 참여자와 부담액 정보가 일치하지 않습니다." }
        var total = 0L
        for (amount in shares.values) {
            require(amount >= 0) { "부담액은 음수일 수 없습니다." }
            total = Math.addExact(total, amount)
        }
        require(total == expense.amount) { "부담액 합계가 지출 금액과 다릅니다." }
        return shares
    }

    private fun createTransfers(balances: List<Balance>): List<Transfer> {
        val senders = mutableListOf<RemainingAmount>()
        val recipients = mutableListOf<RemainingAmount>()
        for (balance in balances) {
            val net = balance.netAmount()
            if (net < 0) senders.add(RemainingAmount(balance.memberId, -net))
            else if (net > 0) recipients.add(RemainingAmount(balance.memberId, net))
        }
        val transfers = mutableListOf<Transfer>()
        var senderIndex = 0
        var recipientIndex = 0
        // ID 순서로 연결하므로 결과는 일정하지만 최소 송금 횟수를 보장하지는 않습니다.
        while (senderIndex < senders.size && recipientIndex < recipients.size) {
            val sender = senders[senderIndex]
            val recipient = recipients[recipientIndex]
            val amount = minOf(sender.amount, recipient.amount)
            transfers.add(Transfer(sender.memberId, recipient.memberId, amount))
            sender.amount -= amount
            recipient.amount -= amount
            if (sender.amount == 0L) senderIndex++
            if (recipient.amount == 0L) recipientIndex++
        }
        return transfers
    }

    private class RemainingAmount(val memberId: Long, var amount: Long)
}
