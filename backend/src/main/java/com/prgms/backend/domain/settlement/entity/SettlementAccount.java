package com.prgms.backend.domain.settlement.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "settlement_accounts",
        uniqueConstraints = @UniqueConstraint(columnNames = {"meeting_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SettlementAccount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;
    @Column(name = "member_id", nullable = false)
    private Long memberId;
    @Column(nullable = false, length = 100)
    private String bankName;
    @Column(nullable = false, length = 50)
    private String accountNumber;
    @Column(nullable = false, length = 100)
    private String accountHolder;

    //계좌 정보 입력
    public SettlementAccount(long meetingId, long memberId, String bankName, String accountNumber, String accountHolder) {
        if (meetingId <= 0 || memberId <= 0) throw new IllegalArgumentException("모임/모임원 ID가 필요합니다.");
        if (bankName == null || bankName.isBlank() || bankName.length() > 100
                || accountHolder == null || accountHolder.isBlank() || accountHolder.length() > 100
                || accountNumber == null || accountNumber.length() > 50 || !accountNumber.matches("[0-9]+(-[0-9]+)*")) {
            throw new IllegalArgumentException("정산 계좌 정보를 확인해주세요.");
        }



        this.meetingId = meetingId;
        this.memberId = memberId;
        this.bankName = bankName.strip();
        this.accountNumber = accountNumber;
        this.accountHolder = accountHolder.strip();
    }
}
