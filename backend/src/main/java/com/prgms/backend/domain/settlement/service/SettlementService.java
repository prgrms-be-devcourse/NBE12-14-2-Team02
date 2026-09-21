package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.settlement.dto.SettlementResponse;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.entity.SettlementBalance;
import com.prgms.backend.domain.settlement.entity.SettlementTransfer;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {
    private final MeetingAccessPort meetingAccess;
    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementCalculator calculator;

    @Transactional
    public SettlementResponse confirm(long meetingId, Principal principal) {
        // 모임 잠금은 트랜잭션 종료까지 유지, 확정 중 새 지출 추가x
        MeetingAccessPort.Context context = meetingAccess.requireMember(meetingId, principal);
        if (!context.leader()) {
            throw new SettlementRequestException(403, "모임장만 정산을 확정할 수 있습니다.");
        }

        // 지출 상시 등록,모임장의 정산 확정 요청을 통해 정산 데이터 생성
        Settlement settlement = settlementRepository.findByMeetingId(meetingId)
                .orElseGet(() -> new Settlement(meetingId));
        settlement.checkOpen();

        List<Expense> expenses = expenseRepository.findByMeetingIdOrderByIdAsc(meetingId);
        SettlementCalculator.Result result = calculator.calculate(meetingId, expenses, context.memberIds());
        List<SettlementBalance> balances = result.balances().stream()
                .map(balance -> new SettlementBalance(balance.memberId(), balance.paidAmount(), balance.shareAmount()))
                .toList();
        List<SettlementTransfer> transfers = result.transfers().stream()
                .map(transfer -> new SettlementTransfer(transfer.senderId(), transfer.recipientId(), transfer.amount()))
                .toList();

        // 결과 저장과 상태 변경은 함께 성공 또는 취소
        settlement.saveResult(balances, transfers);
        settlement.close(context.memberId());
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    @Transactional
    public SettlementResponse getResult(long meetingId, Principal principal) {
        meetingAccess.requireMember(meetingId, principal);
        Settlement settlement = settlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new SettlementRequestException(404, "아직 확정된 정산이 없습니다."));
        if (settlement.getStatus() != SettlementStatus.CLOSED) {
            throw new SettlementRequestException(404, "아직 확정된 정산이 없습니다.");
        }
        return SettlementResponse.from(settlement);
    }
}
