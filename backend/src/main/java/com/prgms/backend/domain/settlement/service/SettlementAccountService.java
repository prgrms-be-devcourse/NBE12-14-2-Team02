package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.settlement.dto.*;
import com.prgms.backend.domain.settlement.entity.SettlementAccount;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementAccountRepository;
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
    private final Validator validator;

    @Transactional(readOnly = true)
    public AccountResponse get(long meetingId, Principal principal) {
        var context = access.requireMemberForRead(meetingId, principal);
        return accounts.findByMeetingIdAndMemberId(meetingId, context.memberId())
                .map(AccountResponse::from).orElse(null);
    }

    @Transactional
    public AccountResponse save(long meetingId, Principal principal, AccountRequest request) {
        // 정산 확정과 같은 잠금: 확정 결과에는 변경 전 또는 변경 후 계좌가 일관되게 복사됩니다.
        var context = access.requireMember(meetingId, principal);
        if (!context.meetingOpen()) throw new SettlementRequestException(409, "종료된 모임의 계좌는 변경할 수 없습니다.");
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new SettlementRequestException(400, "은행명, 계좌번호와 예금주를 확인해주세요.");
        }
        var account = accounts.findByMeetingIdAndMemberId(meetingId, context.memberId())
                .orElseGet(() -> new SettlementAccount(meetingId, context.memberId(), request.bankName(), request.accountNumber(), request.accountHolder()));
        account.update(request.bankName(), request.accountNumber(), request.accountHolder());
        return AccountResponse.from(accounts.save(account));
    }
}
