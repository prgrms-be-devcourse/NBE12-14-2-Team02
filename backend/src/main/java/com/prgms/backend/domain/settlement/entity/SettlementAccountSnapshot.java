package com.prgms.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementAccountSnapshot {
    private long memberId;
    @Column(length = 100) private String bankName;
    @Column(length = 50) private String accountNumber;
    @Column(length = 100) private String accountHolder;

    public SettlementAccountSnapshot(SettlementAccount account) {
        memberId = account.getMemberId();
        bankName = account.getBankName();
        accountNumber = account.getAccountNumber();
        accountHolder = account.getAccountHolder();
    }
}
