package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.settlement.dto.SettlementResponse;
import com.prgms.backend.domain.settlement.dto.SettlementPreviewResponse;
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
    public SettlementPreviewResponse preview(long meetingId, Principal principal) {
        // 지출 변경과 동일한 잠금으로 일관된 예상 결과를 계산하되 저장하지 않습니다.
        var context = meetingAccess.requireMember(meetingId, principal);
        if (!context.meetingOpen()) throw new SettlementRequestException(409, "종료된 모임입니다.");
        settlementRepository.findByMeetingId(meetingId).ifPresent(Settlement::checkOpen);
        var result = calculator.calculate(meetingId, expenseRepository.findByMeetingIdOrderByIdAsc(meetingId), context.memberIds());
        var registered = accountRepository.findByMeetingId(meetingId).stream().map(SettlementAccount::getMemberId).collect(Collectors.toSet());
        var missing = result.transfers().stream().map(SettlementCalculator.Transfer::recipientId).distinct().sorted()
                .filter(id -> !registered.contains(id)).toList();
        var names = memberRepository.findAllById(missing).stream().collect(Collectors.toMap(
                MeetingMember::getId, member -> member.getUser().getNickname()));
        return new SettlementPreviewResponse(result.balances(), result.transfers(), missing.stream()
                .map(id -> new SettlementPreviewResponse.MissingAccount(id, names.getOrDefault(id, "모임원 #" + id))).toList());
    }

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
        // 계좌 미등록은 확정을 막지 않습니다. 등록된 수취인 계좌만 보관합니다.
        var snapshots = recipients.stream().filter(accounts::containsKey)
                .map(id -> new SettlementAccountSnapshot(accounts.get(id))).toList();
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
        return SettlementResponse.forMember(settlementRepository.save(settlement), context.memberId(), false);
    }

    @Transactional(readOnly = true)
    public SettlementResponse getResult(long meetingId, Principal principal) {
        var context = meetingAccess.requireMemberForRead(meetingId, principal);
        Settlement settlement = settlementRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new SettlementRequestException(404, "아직 확정된 정산이 없습니다."));
        if (settlement.getStatus() != SettlementStatus.CLOSED) {
            throw new SettlementRequestException(404, "아직 확정된 정산이 없습니다.");
        }
        return SettlementResponse.forMember(settlement, context.memberId(), false);
    }
}
