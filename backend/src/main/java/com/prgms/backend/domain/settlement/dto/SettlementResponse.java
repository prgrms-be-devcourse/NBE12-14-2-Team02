package com.prgms.backend.domain.settlement.dto;

import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Balance;
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Transfer;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(Long settlementId, Long meetingId, SettlementStatus status,
                                 Long closedByMemberId, LocalDateTime closedAt,
                                 List<Balance> balances, List<Transfer> transfers, List<AccountResponse> accounts,
                                 List<Long> missingAccountMemberIds) {
    public static SettlementResponse forMember(Settlement settlement, long memberId, boolean personalOnly) {
        List<Balance> balances = settlement.getBalances().stream()
                .map(balance -> new Balance(balance.getMemberId(), balance.getPaidAmount(), balance.getShareAmount()))
                .filter(balance -> !personalOnly || balance.memberId() == memberId)
                .toList();
        List<Transfer> transfers = settlement.getTransfers().stream()
                .map(transfer -> new Transfer(transfer.getSenderId(), transfer.getRecipientId(), transfer.getAmount()))
                .filter(transfer -> !personalOnly || transfer.senderId() == memberId || transfer.recipientId() == memberId)
                .toList();
        return new SettlementResponse(settlement.getId(), settlement.getMeetingId(), settlement.getStatus(),
                settlement.getClosedByMemberId(), settlement.getClosedAt(), balances, transfers,
                settlement.getAccounts().stream()
                        .filter(account -> transfers.stream().anyMatch(transfer -> transfer.senderId() == memberId && transfer.recipientId() == account.getMemberId()))
                        .map(account -> new AccountResponse(account.getMemberId(),
                        account.getBankName(), account.getAccountNumber(), account.getAccountHolder())).toList(),
                transfers.stream().map(Transfer::recipientId).distinct().sorted()
                        .filter(id -> settlement.getAccounts().stream().noneMatch(account -> account.getMemberId() == id)).toList());
    }
}
