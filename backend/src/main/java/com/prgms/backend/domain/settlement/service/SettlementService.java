package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.settlement.dto.SettlementResponse;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.entity.SettlementBalance;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.entity.SettlementTransfer;
import com.prgms.backend.domain.settlement.entity.SettlementAccount;
import com.prgms.backend.domain.settlement.entity.SettlementAccountSnapshot;
import com.prgms.backend.domain.settlement.repository.SettlementAccountRepository;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import java.util.stream.Collectors;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import lombok.RequiredArgsConstructor;
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
    private final NotificationService notificationService;
    private final SettlementAccountRepository accountRepository;
    private final MeetingMemberRepository memberRepository;

    @Transactional
    public SettlementResponse confirm(long meetingId, Principal principal) {
        // 모임 잠금은 트랜잭션 종료까지 유지, 확정 중 새 지출 추가x
        MeetingAccessPort.Context context = meetingAccess.requireMember(meetingId, principal);
        if (!context.leader()) {
            throw new SettlementRequestException(403, "모임장만 정산을 확정할 수 있습니다.");
        }
        if (!context.meetingOpen()) {
            throw new SettlementRequestException(409, "종료된 모임은 정산을 확정할 수 없습니다.");
        }

        // 지출 상시 등록,모임장의 정산 확정 요청을 통해 정산 데이터 생성
        Settlement settlement = settlementRepository.findByMeetingId(meetingId)
                .orElseGet(() -> new Settlement(meetingId));
        settlement.checkOpen();

        List<Expense> expenses = expenseRepository.findByMeetingIdOrderByIdAsc(meetingId);
        SettlementCalculator.Result result = calculator.calculate(meetingId, expenses, context.memberIds());
        var accounts = accountRepository.findByMeetingId(meetingId).stream()
                .collect(Collectors.toMap(SettlementAccount::getMemberId, account -> account));
        var recipients = result.transfers().stream().map(SettlementCalculator.Transfer::recipientId).distinct().sorted().toList();
        var missing = recipients.stream().filter(id -> !accounts.containsKey(id)).toList();
        if (!missing.isEmpty()) {
            var names = memberRepository.findAllById(missing).stream().collect(Collectors.toMap(
                    MeetingMember::getId, member -> member.getUser().getNickname()));
            String message = missing.stream().map(id -> names.getOrDefault(id, "모임원 #" + id))
                    .collect(Collectors.joining(", "));
            throw new SettlementRequestException(409, message + "님의 정산 계좌 등록이 필요합니다. 등록 후 다시 확정해주세요.");
        }
        var snapshots = recipients.stream().map(id -> new SettlementAccountSnapshot(accounts.get(id))).toList();
        List<SettlementBalance> balances = result.balances().stream()
                .map(balance -> new SettlementBalance(balance.memberId(), balance.paidAmount(), balance.shareAmount()))
                .toList();
        List<SettlementTransfer> transfers = result.transfers().stream()
                .map(transfer -> new SettlementTransfer(transfer.senderId(), transfer.recipientId(), transfer.amount()))
                .toList();

        // 결과 저장과 상태 변경은 함께 성공 또는 취소
        settlement.saveResult(balances, transfers);
        settlement.saveAccounts(snapshots);
        settlement.close(context.memberId());
        notificationService.notifySettlementClosed(meetingId);
        return SettlementResponse.from(settlementRepository.save(settlement));
    }

    @Transactional(readOnly = true)
    public SettlementResponse getResult(long meetingId, Principal principal) {
        meetingAccess.requireMemberForRead(meetingId, principal);
        Settlement settlement = settlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new SettlementRequestException(404, "아직 확정된 정산이 없습니다."));
        if (settlement.getStatus() != SettlementStatus.CLOSED) {
            throw new SettlementRequestException(404, "아직 확정된 정산이 없습니다.");
        }
        return SettlementResponse.from(settlement);
    }
}
