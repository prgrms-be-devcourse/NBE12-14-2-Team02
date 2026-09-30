package com.prgms.backend.domain.settlement.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

//정산 결과 안내
@Embeddable
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementTransfer {
    private long senderId;//보내는사람
    private long recipientId;//받는사람
    private long amount;//금액
}
