package com.prgms.backend.domain.settlement.service

import com.prgms.backend.domain.settlement.dto.*
import com.prgms.backend.domain.settlement.entity.SettlementAccount
import com.prgms.backend.domain.settlement.entity.SettlementStatus
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort
import com.prgms.backend.domain.settlement.repository.*
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException
import jakarta.validation.Validator
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal

@Service
class SettlementAccountService(
    private val access: MeetingAccessPort,
    private val accounts: SettlementAccountRepository,
    private val settlements: SettlementRepository,
    private val validator: Validator
) {
    @Transactional(readOnly = true)
    fun get(meetingId: Long, principal: Principal?): AccountResponse? {
        val context = access.requireMemberForRead(meetingId, principal)
        return accounts.findByMeetingIdAndMemberId(meetingId, context.memberId).map(AccountResponse::from).orElse(null)
    }

    @Transactional
    fun save(meetingId: Long, principal: Principal?, request: AccountRequest?): AccountResponse {
        val context = access.requireMember(meetingId, principal)
        if (!context.meetingOpen) throw SettlementRequestException(409, "종료된 모임의 계좌는 변경할 수 없습니다.")
        val settlement = settlements.findByMeetingId(meetingId).orElse(null)
        val closed = settlement?.status == SettlementStatus.CLOSED
        if (closed && !requireNotNull(settlement).canRegisterMissingAccount(context.memberId))
            throw SettlementRequestException(409, "확정 후에는 미등록 수취인의 계좌만 등록할 수 있습니다.")
        if (request == null || validator.validate(request).isNotEmpty())
            throw SettlementRequestException(400, "은행명, 계좌번호와 예금주를 확인해주세요.")
        val account = accounts.findByMeetingIdAndMemberId(meetingId, context.memberId).orElseGet {
            SettlementAccount(meetingId, context.memberId, request.bankName, request.accountNumber, request.accountHolder)
        }
        account.update(request.bankName, request.accountNumber, request.accountHolder)
        val saved = accounts.save(account)
        if (closed) requireNotNull(settlement).registerMissingAccount(saved)
        return AccountResponse.from(saved)
    }
}
