package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.settlement.dto.*;
import com.prgms.backend.domain.settlement.entity.SettlementAccount;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementAccountRepository;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;

@Service
@RequiredArgsConstructor
public class SettlementAccountService {
    private final MeetingAccessPort access;
    private final SettlementAccountRepository accounts;
    private final SettlementRepository settlements;
    private final Validator validator;

    @Transactional(readOnly = true)
    public AccountResponse get(long meetingId, Principal principal) {
        var context = access.requireMemberForRead(meetingId, principal);
        return accounts.findByMeetingIdAndMemberId(meetingId, context.memberId())
                .map(AccountResponse::from).orElse(null);
    }

    @Transactional
    public AccountResponse save(long meetingId, Principal principal, AccountRequest request) {
        // 정산 확정과 같은 모임 잠금을 잡은 뒤 확정 여부를 확인합니다.
        var context = access.requireMember(meetingId, principal);
        if (!context.meetingOpen()) throw new SettlementRequestException(409, "종료된 모임의 계좌는 변경할 수 없습니다.");
        var settlement = settlements.findByMeetingId(meetingId).orElse(null);
        boolean closed = settlement != null && settlement.getStatus() == SettlementStatus.CLOSED;
        if (closed && !settlement.canRegisterMissingAccount(context.memberId())) {
            throw new SettlementRequestException(409, "확정 후에는 미등록 수취인의 계좌만 등록할 수 있습니다.");
        }
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new SettlementRequestException(400, "은행명, 계좌번호와 예금주를 확인해주세요.");
        }
        var account = accounts.findByMeetingIdAndMemberId(meetingId, context.memberId())
                .orElseGet(() -> new SettlementAccount(meetingId, context.memberId(), request.bankName(), request.accountNumber(), request.accountHolder()));
        account.update(request.bankName(), request.accountNumber(), request.accountHolder());
        var saved = accounts.save(account);
        if (closed) settlement.registerMissingAccount(saved);
        return AccountResponse.from(saved);
    }
}
