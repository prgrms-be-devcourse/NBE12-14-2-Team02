package com.prgms.backend.domain.settlement.dto;

import com.prgms.backend.domain.settlement.entity.SettlementAccount;

public record AccountResponse(long memberId, String bankName, String accountNumber, String accountHolder) {
    public static AccountResponse from(SettlementAccount account) {
        return new AccountResponse(account.getMemberId(), account.getBankName(), account.getAccountNumber(), account.getAccountHolder());
    }
}
