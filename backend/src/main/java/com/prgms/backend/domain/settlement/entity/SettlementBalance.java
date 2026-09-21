package com.prgms.backend.domain.settlement.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 인당 지불해야 할 금액,받을 금액 계산
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementBalance {
    private long memberId;
    private long paidAmount;
    private long shareAmount;
}
