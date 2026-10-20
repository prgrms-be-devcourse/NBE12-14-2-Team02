package com.prgms.backend.domain.settlement.service

import com.prgms.backend.domain.expense.repository.ExpenseRepository
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository
import com.prgms.backend.domain.settlement.dto.*
import com.prgms.backend.domain.settlement.entity.*
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort
import com.prgms.backend.domain.settlement.integration.SettlementClosedEvent
import com.prgms.backend.domain.settlement.repository.*
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal

@Service
class SettlementService(
    private val meetingAccess: MeetingAccessPort,
    private val expenseRepository: ExpenseRepository,
    private val settlementRepository: SettlementRepository,
    private val calculator: SettlementCalculator,
    private val eventPublisher: ApplicationEventPublisher,
    private val accountRepository: SettlementAccountRepository,
    private val memberRepository: MeetingMemberRepository
) {
    @Transactional
    fun preview(meetingId: Long, principal: Principal?): SettlementPreviewResponse {
        // 미리보기 역시 지출 변경과 같은 모임 잠금 안에서 일관된 값을 읽습니다.
        val context = meetingAccess.requireMember(meetingId, principal)
        if (!context.meetingOpen) throw SettlementRequestException(409, "종료된 모임입니다.")
        settlementRepository.findByMeetingId(meetingId).ifPresent { it.checkOpen() }
        val result = calculator.calculate(meetingId, expenseRepository.findByMeetingIdOrderByIdAsc(meetingId), context.memberIds)
        val registered = accountRepository.findByMeetingId(meetingId).map { it.memberId }.toSet()
        val missing = missingRecipients(result, registered)
        val names = memberRepository.findAllById(missing).associate { it.id to it.user.nickname }
        return SettlementPreviewResponse(result.balances, result.transfers,
            missing.map { SettlementPreviewResponse.MissingAccount(it, names[it] ?: "모임원 #$it") })
    }

    @Transactional
    fun confirm(meetingId: Long, principal: Principal?): SettlementResponse {
        val context = meetingAccess.requireMember(meetingId, principal)
        if (!context.leader) throw SettlementRequestException(403, "모임장만 정산을 확정할 수 있습니다.")
        if (!context.meetingOpen) throw SettlementRequestException(409, "종료된 모임은 정산을 확정할 수 없습니다.")
        val settlement = settlementRepository.findByMeetingId(meetingId).orElseGet { Settlement(meetingId) }
        settlement.checkOpen()
        val result = calculator.calculate(meetingId, expenseRepository.findByMeetingIdOrderByIdAsc(meetingId), context.memberIds)
        val accounts = accountRepository.findByMeetingId(meetingId).associateBy { it.memberId }
        val missing = missingRecipients(result, accounts.keys)
        if (missing.isNotEmpty()) throw SettlementRequestException(409,
            "계좌를 등록하지 않은 정산 수취인이 있습니다. 모임원 ID: $missing")
        val recipients = result.transfers.map { it.recipientId }.distinct().sorted()
        val snapshots = recipients.map { SettlementAccountSnapshot(accounts.getValue(it)) }
        val balances = result.balances.map { SettlementBalance(it.memberId, it.paidAmount, it.shareAmount) }
        val transfers = result.transfers.map { SettlementTransfer(it.senderId, it.recipientId, it.amount) }
        // 결과와 상태는 한 트랜잭션에 저장하고, 알림만 커밋 이후 별도로 저장합니다.
        settlement.saveResult(balances, transfers)
        settlement.saveAccounts(snapshots)
        settlement.close(context.memberId)
        val saved = settlementRepository.save(settlement)
        eventPublisher.publishEvent(SettlementClosedEvent(meetingId, requireNotNull(saved.id), requireNotNull(saved.closedAt)))
        return SettlementResponse.forMember(saved, context.memberId, false)
    }

    private fun missingRecipients(result: SettlementCalculator.Result, registered: Set<Long>): List<Long> =
        result.transfers.map { it.recipientId }.distinct().sorted().filter { it !in registered }

    @Transactional(readOnly = true)
    fun getResult(meetingId: Long, principal: Principal?): SettlementResponse {
        val context = meetingAccess.requireMemberForRead(meetingId, principal)
        val settlement = settlementRepository.findByMeetingId(meetingId)
            .orElseThrow { SettlementRequestException(404, "아직 확정된 정산이 없습니다.") }
        if (settlement.status != SettlementStatus.CLOSED) throw SettlementRequestException(404, "아직 확정된 정산이 없습니다.")
        return SettlementResponse.forMember(settlement, context.memberId, false)
    }
}
