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

    // 인증된 영수증 API에서 사용하는 비공개 로컬 저장소의 파일 키 (지출당 1장)
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
    private String splitMode; //정산 나눌 방식 -> 지정 또는 모임 전체 인원수만큼
    private Integer roundingUnit; //1000,100,10단위로 금액을 나눔
    private Long remainderMemberId; //나머지 멤버

    // 정산금액
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
    //어느 모임에서 발생한 참여자와 총 금액,메모 내용
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

    // 결제자와 등록 시각은 유지하고, 수정한 분담 결과만 교체합니다.
    public void update(String title, long amount, String memo, Map<Long, Long> shares,
                       String splitMode, Integer roundingUnit, Long remainderMemberId) {
        this.title = title.strip();
        this.amount = amount;
        this.memo = memo;
        this.participantIds.clear();
        this.participantIds.addAll(shares.keySet());
        this.participantAmounts.clear();
        this.participantAmounts.putAll(shares);
        this.splitMode = splitMode;
        this.roundingUnit = roundingUnit;
        this.remainderMemberId = remainderMemberId;
    }

    // null이면 영수증 첨부를 해제합니다.
    public void attachReceipt(String storageKey) {
        receiptKey = storageKey;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
