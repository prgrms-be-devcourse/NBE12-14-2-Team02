package com.prgms.backend.domain.settlement.dto;

import com.prgms.backend.domain.settlement.service.SettlementCalculator;
import java.util.List;

public record SettlementPreviewResponse(List<SettlementCalculator.Balance> balances,
        List<SettlementCalculator.Transfer> transfers, List<MissingAccount> missingAccounts) {
    public record MissingAccount(long memberId, String nickname) {}
}
