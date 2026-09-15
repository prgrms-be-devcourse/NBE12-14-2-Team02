package com.prgms.backend.domain.expense.dto;

import com.prgms.backend.domain.expense.entity.Expense;
import java.time.LocalDateTime;
import java.util.List;

public record ExpenseResponse(Long id, Long meetingId, Long payerMemberId, String title,
                              long amount, String memo, String splitMode, Integer roundingUnit,
                              Long remainderMemberId, List<Share> participants, LocalDateTime createdAt) {
    public record Share(long memberId, long amount) {}
    public static ExpenseResponse from(Expense e) {
        var shares = e.getParticipantAmounts().entrySet().stream().sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> new Share(entry.getKey(), entry.getValue())).toList();
        return new ExpenseResponse(e.getId(), e.getMeetingId(), e.getPayerMemberId(), e.getTitle(),
                e.getAmount(), e.getMemo(), e.getSplitMode(), e.getRoundingUnit(),
                e.getRemainderMemberId(), shares, e.getCreatedAt());
    }
}
