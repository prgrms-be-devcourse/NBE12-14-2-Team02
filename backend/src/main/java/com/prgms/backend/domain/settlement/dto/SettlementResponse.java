package com.prgms.backend.domain.settlement.dto;

import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Balance;
import com.prgms.backend.domain.settlement.service.SettlementCalculator.Transfer;
import java.time.LocalDateTime;
import java.util.List;

public record SettlementResponse(Long settlementId, Long meetingId, SettlementStatus status,
                                 Long closedByMemberId, LocalDateTime closedAt,
                                 List<Balance> balances, List<Transfer> transfers) {
    public static SettlementResponse from(Settlement settlement) {
        List<Balance> balances = settlement.getBalances().stream()
                .map(balance -> new Balance(balance.getMemberId(), balance.getPaidAmount(), balance.getShareAmount()))
                .toList();
        List<Transfer> transfers = settlement.getTransfers().stream()
                .map(transfer -> new Transfer(transfer.getSenderId(), transfer.getRecipientId(), transfer.getAmount()))
                .toList();
        return new SettlementResponse(settlement.getId(), settlement.getMeetingId(), settlement.getStatus(),
                settlement.getClosedByMemberId(), settlement.getClosedAt(), balances, transfers);
    }
}
