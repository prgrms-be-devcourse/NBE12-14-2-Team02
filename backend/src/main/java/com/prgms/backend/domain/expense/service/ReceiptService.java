package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.exception.ExpenseRequestException;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;

@Service
@RequiredArgsConstructor
public class ReceiptService {
    private final ExpenseRepository expenses;
    private final MeetingAccessPort access;
    private final SettlementRepository settlements;
    private final ReceiptStorage storage;

    @Transactional(readOnly = true)
    public void checkUpload(long meetingId, long expenseId, Principal principal) {
        var context = access.requireMemberForRead(meetingId, principal);
        checkEditable(meetingId, expenseId, context);
    }

    @Transactional
    public void replace(long meetingId, long expenseId, Principal principal, String key) {
        var context = access.requireMember(meetingId, principal);
        Expense expense = checkEditable(meetingId, expenseId, context);
        String old = expense.getReceiptKey();
        expense.attachReceipt(key);
        storage.deleteAfterCommit(old);
    }

    @Transactional(readOnly = true)
    public byte[] read(long meetingId, long expenseId, Principal principal) {
        access.requireMemberForRead(meetingId, principal);
        return storage.read(find(meetingId, expenseId).getReceiptKey());
    }

    private Expense checkEditable(long meetingId, long expenseId, MeetingAccessPort.Context context) {
        if (!context.meetingOpen()) throw new ExpenseRequestException(409, "종료된 모임의 영수증은 변경할 수 없습니다.");
        settlements.findByMeetingId(meetingId).ifPresent(settlement -> settlement.checkOpen());
        Expense expense = find(meetingId, expenseId);
        if (expense.getPayerMemberId() != context.memberId()) throw new ExpenseRequestException(403, "본인 지출의 영수증만 변경할 수 있습니다.");
        return expense;
    }

    private Expense find(long meetingId, long expenseId) {
        return expenses.findById(expenseId).filter(e -> e.getMeetingId() == meetingId)
                .orElseThrow(() -> new ExpenseRequestException(404, "해당 모임의 지출을 찾을 수 없습니다."));
    }
}
