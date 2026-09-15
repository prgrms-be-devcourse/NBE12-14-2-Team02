package com.prgms.backend.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.*;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long meetingId;
    @Column(nullable = false)
    private Long payerMemberId;
    @Column(nullable = false, length = 255)
    private String title;
    @Column(nullable = false)
    private long amount;
    @Column(length = 2000)
    private String memo;

    //관련 첨부파일 관련해서는 추후 수정 예정
    //영수증은 파일 경로로 우선 저장, 지금은 DB에는 경로만,나중에 영수증 파일 자체를 저장하도록 수정 예쩡
    @Column(length = 500)
    private String receiptKey;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection //
    @CollectionTable(name = "expense_participants", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "member_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();

    //각각 얼마 내야 하는지 할당
    @ElementCollection
    @CollectionTable(name = "expense_shares", joinColumns = @JoinColumn(name = "expense_id"))
    @MapKeyColumn(name = "member_id")
    @Column(name = "share_amount", nullable = false)
    private Map<Long, Long> participantAmounts = new HashMap<>();


    @Column(length = 10)
    private String splitMode;
    private Integer roundingUnit;
    private Long remainderMemberId;


    public Map<Long, Long> getParticipantAmounts() { return Map.copyOf(participantAmounts); }
    //정산금액 나누는 방식 n분의1, 지정 등록 -> 지정 등록의 금액 지정 기능 추가 예정
    public static Expense register(long meetingId, long payerMemberId, String title, long amount, String memo,
                                   Map<Long, Long> shares, String splitMode, Integer roundingUnit, Long remainderMemberId) {
        Expense expense = new Expense(meetingId, payerMemberId, title, amount, memo, shares.keySet());
        expense.participantAmounts.putAll(shares);
        expense.splitMode = splitMode;
        expense.roundingUnit = roundingUnit;
        expense.remainderMemberId = remainderMemberId;
        return expense;
    }

    public Expense(long meetingId, long payerMemberId, String title, long amount,
                   String memo, Set<Long> participantIds) {
        this.meetingId = meetingId;
        this.payerMemberId = payerMemberId;
        this.title = title.strip();
        this.amount = amount;
        this.memo = memo;
        this.participantIds.addAll(participantIds);
    }

    public Set<Long> getParticipantIds() { return Set.copyOf(participantIds); }

    // 영수증
    public void attachReceipt(String storageKey) {
        receiptKey = storageKey;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
