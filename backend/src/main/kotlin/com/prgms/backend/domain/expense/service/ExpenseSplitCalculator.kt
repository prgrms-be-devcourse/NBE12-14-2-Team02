package com.prgms.backend.domain.expense.service

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest
import com.prgms.backend.domain.expense.exception.ExpenseRequestException
import org.springframework.stereotype.Component
import java.util.concurrent.ThreadLocalRandom

@Component
class ExpenseSplitCalculator {
    class Result(shares: Map<Long, Long>, val remainderMemberId: Long?) {
        val shares: Map<Long, Long> = java.util.Map.copyOf(shares)
        fun shares() = shares
        fun remainderMemberId() = remainderMemberId
    }

    fun calculate(request: ExpenseCreateRequest): Map<Long, Long> = calculateResult(request).shares

    fun calculateResult(request: ExpenseCreateRequest): Result {
        var selectedMemberId: Long? = null
        val total = requireNotNull(request.amount).longValueExact()
        if (total <= 0) throw invalid("지출 금액은 1원 이상이어야 합니다.")
        val participants = requireNotNull(request.participants).map { requireNotNull(it) }
        val shares = sortedMapOf<Long, Long>()
        for (participant in participants) {
            if (shares.putIfAbsent(requireNotNull(participant.memberId), 0L) != null)
                throw invalid("참여자가 중복되었습니다.")
        }
        if (shares.isEmpty()) throw invalid("참여자를 한 명 이상 선택해주세요.")
        when (requireNotNull(request.splitMode)) {
            ExpenseCreateRequest.SplitMode.EXACT -> {
                if (request.roundingUnit != null || request.remainderMemberId != null || request.randomRemainder)
                    throw invalid("금액 지정 분담에는 분담 단위와 차액 담당자를 지정할 수 없습니다.")
                var sum = 0L
                for (participant in participants) {
                    val amount = participant.amount?.longValueExact() ?: throw invalid("각 참여자의 부담 금액을 입력해주세요.")
                    if (amount < 0 || amount > total) throw invalid("개인 부담액은 0원 이상, 지출 금액 이하여야 합니다.")
                    sum = Math.addExact(sum, amount)
                    shares[requireNotNull(participant.memberId)] = amount
                }
                if (sum != total) throw invalid("개인 부담액의 합계가 지출 금액과 같아야 합니다.")
            }
            ExpenseCreateRequest.SplitMode.EQUAL -> {
                if (request.randomRemainder && request.remainderMemberId != null)
                    throw invalid("차액 담당자 직접 지정과 랜덤 선택 중 하나만 사용해주세요.")
                val unit = request.roundingUnit ?: 1
                if (unit !in setOf(1, 10, 100, 1000)) throw invalid("분담 단위는 1, 10, 100, 1000원 중 선택해주세요.")
                if (participants.any { it.amount != null }) throw invalid("균등 분담에는 개인 금액을 입력하지 마세요.")
                if (request.remainderMemberId != null && request.remainderMemberId !in shares)
                    throw invalid("차액 담당자는 해당 지출 참여자여야 합니다.")
                val each = total / shares.size / unit * unit
                val remainder = total - each * shares.size
                shares.replaceAll { _, _ -> each }
                if (remainder > 0) {
                    selectedMemberId = if (request.randomRemainder) {
                        val candidates = shares.keys.toList()
                        candidates[ThreadLocalRandom.current().nextInt(candidates.size)]
                    } else request.remainderMemberId ?: throw invalid("남은 차액을 부담할 참여자를 선택해주세요.")
                    shares[selectedMemberId] = each + remainder
                }
            }
        }
        return Result(shares, selectedMemberId)
    }

    private fun invalid(message: String) = ExpenseRequestException(400, message)
}
