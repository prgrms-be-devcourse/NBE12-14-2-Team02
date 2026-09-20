package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.expense.entity.Expense;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class SettlementCalculator {
    public record Balance(long memberId, long paidAmount, long shareAmount) {
        // 정산 금액 최종 계산, 양수면 받을 돈, 음수면 보낼 돈
        public long netAmount() {
            return paidAmount - shareAmount;
        }
    }

    public record Transfer(long senderId, long recipientId, long amount) {}

    public record Result(List<Balance> balances, List<Transfer> transfers) {
        public Result {
            balances = List.copyOf(balances);
            transfers = List.copyOf(transfers);
        }
    }

    public Result calculate(long meetingId, List<Expense> expenses, Set<Long> memberIds) {
        if (meetingId <= 0 || expenses == null || memberIds == null || memberIds.isEmpty()
                || memberIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("모임과 참여자 정보가 필요합니다.");
        }

        Map<Long, Long> paidAmounts = new TreeMap<>();
        Map<Long, Long> shareAmounts = new TreeMap<>();
        for (long memberId : memberIds) {
            paidAmounts.put(memberId, 0L);
            shareAmounts.put(memberId, 0L);
        }

        Set<Expense> seenExpenses = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Long> seenIds = new HashSet<>();
        for (Expense expense : expenses) {
            if (expense == null || !seenExpenses.add(expense)
                    || (expense.getId() != null && !seenIds.add(expense.getId()))) {
                throw new IllegalArgumentException("지출이 비어 있거나 중복되었습니다.");
            }
            validateExpense(meetingId, expense, memberIds);
            long payerId = expense.getPayerMemberId();
            paidAmounts.put(payerId, Math.addExact(paidAmounts.get(payerId), expense.getAmount()));

            // 다시 균등 분배하지 않고, 지출 등록 시 결정한 부담액을 그대로 합산합니다.
            for (Map.Entry<Long, Long> share : getShares(expense).entrySet()) {
                long memberId = share.getKey();
                shareAmounts.put(memberId, Math.addExact(shareAmounts.get(memberId), share.getValue()));
            }
        }

        List<Balance> balances = new ArrayList<>();
        for (long memberId : paidAmounts.keySet()) {
            balances.add(new Balance(memberId, paidAmounts.get(memberId), shareAmounts.get(memberId)));
        }
        return new Result(balances, createTransfers(balances));
    }

    private void validateExpense(long meetingId, Expense expense, Set<Long> memberIds) {
        if (expense.getMeetingId() == null || expense.getMeetingId() != meetingId
                || expense.getAmount() <= 0 || !memberIds.contains(expense.getPayerMemberId())
                || expense.getParticipantIds().isEmpty()
                || !memberIds.containsAll(expense.getParticipantIds())) {
            throw new IllegalArgumentException("모임 또는 지출 참여자 정보를 확인해주세요.");
        }
    }

    private Map<Long, Long> getShares(Expense expense) {
        Map<Long, Long> shares = expense.getParticipantAmounts();
        if (shares.isEmpty() && expense.getSplitMode() == null) {
            // 개인별 금액을 저장하기 전에 등록된 기존 지출에만 과거 균등 분배 규칙 적용
            List<Long> participants = expense.getParticipantIds().stream().sorted().toList();
            Map<Long, Long> legacyShares = new TreeMap<>();
            long eachAmount = expense.getAmount() / participants.size();
            long remainder = expense.getAmount() % participants.size();
            for (int index = 0; index < participants.size(); index++) {
                legacyShares.put(participants.get(index), eachAmount + (index < remainder ? 1 : 0));
            }
            return legacyShares;
        }
        if (!shares.keySet().equals(expense.getParticipantIds())) {
            throw new IllegalArgumentException("지출 참여자와 부담액 정보가 일치하지 않습니다.");
        }
        long total = 0;
        for (long amount : shares.values()) {
            if (amount < 0) {
                throw new IllegalArgumentException("부담액은 음수일 수 없습니다.");
            }
            total = Math.addExact(total, amount);
        }
        if (total != expense.getAmount()) {
            throw new IllegalArgumentException("부담액 합계가 지출 금액과 다릅니다.");
        }
        return shares;
    }

    //정산할 사람 정하기
    private List<Transfer> createTransfers(List<Balance> balances) {
        List<RemainingAmount> senders = new ArrayList<>();
        List<RemainingAmount> recipients = new ArrayList<>();
        for (Balance balance : balances) {
            long netAmount = balance.netAmount();
            if (netAmount < 0) {
                senders.add(new RemainingAmount(balance.memberId(), -netAmount));
            } else if (netAmount > 0) {
                recipients.add(new RemainingAmount(balance.memberId(), netAmount));
            }
        }

    // 돈 받을 사람과 보내는 사람의 금액을 비교해서 정산 진행
        List<Transfer> transfers = new ArrayList<>();
        int senderIndex = 0;
        int recipientIndex = 0;
        // 둘 중 한사람의 돈이 0원(보내거나 받는돈)이 되면 다음 사람 진행
        // ID 순서로 연결하므로 결과가 일정하지만, 최소 송금 횟수를 보장x
        while (senderIndex < senders.size() && recipientIndex < recipients.size()) {
            RemainingAmount sender = senders.get(senderIndex);
            RemainingAmount recipient = recipients.get(recipientIndex);
            long amount = Math.min(sender.amount, recipient.amount);
            transfers.add(new Transfer(sender.memberId, recipient.memberId, amount));
            sender.amount -= amount;
            recipient.amount -= amount;
            if (sender.amount == 0) {
                senderIndex++;
            }
            if (recipient.amount == 0) {
                recipientIndex++;
            }
        }
        return transfers;
    }

    // 이름으로 모임원과 남은 금액을 구분
    private static class RemainingAmount {
        private final long memberId;
        private long amount;

        private RemainingAmount(long memberId, long amount) {
            this.memberId = memberId;
            this.amount = amount;
        }
    }
}
