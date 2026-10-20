package com.prgms.backend.domain.expense.service

import com.prgms.backend.domain.expense.dto.*
import com.prgms.backend.domain.expense.entity.Expense
import com.prgms.backend.domain.expense.exception.ExpenseRequestException
import com.prgms.backend.domain.expense.repository.ExpenseRepository
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository
import com.prgms.backend.domain.settlement.entity.SettlementStatus
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort
import com.prgms.backend.domain.settlement.repository.SettlementRepository
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal

@Service
class ExpenseService(
    private val expenseRepository: ExpenseRepository,
    private val splitCalculator: ExpenseSplitCalculator,
    private val meetingAccess: MeetingAccessPort,
    private val settlementRepository: SettlementRepository,
    private val memberRepository: MeetingMemberRepository,
    private val validator: Validator,
    private val receiptStorage: ReceiptStorage
) {
    @Transactional(readOnly = true)
    fun list(meetingId: Long, principal: Principal?): ExpenseListResponse {
        val context = meetingAccess.requireMemberForRead(meetingId, principal)
        val closed = settlementRepository.findByMeetingId(meetingId).map { it.status == SettlementStatus.CLOSED }.orElse(false)
        val expenses = expenseRepository.findByMeetingIdOrderByIdAsc(meetingId).map(ExpenseResponse::from)
        return ExpenseListResponse(context.memberId, context.leader, context.meetingOpen && !closed, expenses)
    }

    @Transactional(readOnly = true)
    fun get(meetingId: Long, expenseId: Long, principal: Principal?): ExpenseResponse {
        meetingAccess.requireMemberForRead(meetingId, principal)
        return ExpenseResponse.from(findExpense(meetingId, expenseId))
    }

    @Transactional
    fun create(meetingId: Long, principal: Principal?, request: ExpenseCreateRequest?): ExpenseResponse {
        if (meetingId <= 0) throw ExpenseRequestException(400, "모임 ID를 확인해주세요.")
        val context = meetingAccess.requireMember(meetingId, principal)
        checkRegistrationAllowed(meetingId, context)
        val checked = validateRequest(request)
        val result = splitCalculator.calculateResult(checked)
        checkParticipants(meetingId, result.shares.keys, emptySet())
        val expense = Expense.register(meetingId, context.memberId, requireNotNull(checked.title),
            requireNotNull(checked.amount).longValueExact(), checked.memo, result.shares,
            requireNotNull(checked.splitMode).name, getRoundingUnit(checked), result.remainderMemberId)
        return ExpenseResponse.from(expenseRepository.save(expense))
    }

    @Transactional
    fun update(meetingId: Long, expenseId: Long, principal: Principal?, request: ExpenseCreateRequest?): ExpenseResponse {
        val context = meetingAccess.requireMember(meetingId, principal)
        checkRegistrationAllowed(meetingId, context)
        val expense = findExpense(meetingId, expenseId)
        checkOwner(expense, context.memberId)
        val checked = validateRequest(request)
        val result = splitCalculator.calculateResult(checked)
        checkParticipants(meetingId, result.shares.keys, expense.getParticipantIds())
        expense.update(requireNotNull(checked.title), requireNotNull(checked.amount).longValueExact(), checked.memo,
            result.shares, requireNotNull(checked.splitMode).name, getRoundingUnit(checked), result.remainderMemberId)
        return ExpenseResponse.from(expense)
    }

    @Transactional
    fun delete(meetingId: Long, expenseId: Long, principal: Principal?) {
        val context = meetingAccess.requireMember(meetingId, principal)
        checkRegistrationAllowed(meetingId, context)
        val expense = findExpense(meetingId, expenseId)
        checkOwner(expense, context.memberId)
        expenseRepository.delete(expense)
        receiptStorage.deleteAfterCommit(expense.receiptKey)
    }

    private fun findExpense(meetingId: Long, expenseId: Long): Expense = expenseRepository.findById(expenseId)
        .filter { it.meetingId == meetingId }.orElseThrow { ExpenseRequestException(404, "해당 모임의 지출을 찾을 수 없습니다.") }

    private fun checkOwner(expense: Expense, memberId: Long) {
        if (expense.payerMemberId != memberId) throw ExpenseRequestException(403, "본인이 등록한 지출만 수정·삭제할 수 있습니다.")
    }

    private fun checkRegistrationAllowed(meetingId: Long, context: MeetingAccessPort.Context) {
        if (!context.meetingOpen) throw ExpenseRequestException(409, "종료된 모임의 지출은 변경할 수 없습니다.")
        settlementRepository.findByMeetingId(meetingId).ifPresent { it.checkOpen() }
    }

    private fun validateRequest(request: ExpenseCreateRequest?): ExpenseCreateRequest {
        if (request == null || validator.validate(request).isNotEmpty()) throw ExpenseRequestException(400, "지출 입력값을 확인해주세요.")
        return request
    }

    private fun checkParticipants(meetingId: Long, participantIds: Set<Long>, existingParticipantIds: Set<Long>) {
        val allowed = existingParticipantIds.toMutableSet()
        memberRepository.findAllByMeetingIdAndStatus(meetingId, MeetingMemberStatus.JOINED).forEach { allowed.add(it.id) }
        if (!allowed.containsAll(participantIds)) throw ExpenseRequestException(400, "새 부담자는 현재 모임에 참여 중인 모임원만 지정할 수 있습니다.")
    }

    private fun getRoundingUnit(request: ExpenseCreateRequest): Int? =
        if (request.splitMode == ExpenseCreateRequest.SplitMode.EQUAL) request.roundingUnit ?: 1 else null
}
