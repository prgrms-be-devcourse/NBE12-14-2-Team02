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
    // One optional attachment. Storage/upload implementation will be added later.
    @Column(length = 500)
    private String receiptKey;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ElementCollection
    @CollectionTable(name = "expense_participants", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "member_id", nullable = false)
    private Set<Long> participantIds = new HashSet<>();

    public Expense(long meetingId, long payerMemberId, String title, long amount,
                   String memo, Set<Long> participantIds) {
        if (meetingId <= 0 || payerMemberId <= 0) throw new IllegalArgumentException("모임/모임원 ID가 필요합니다.");
        if (title == null || title.isBlank() || title.length() > 255) throw new IllegalArgumentException("지출 제목을 확인해주세요.");
        if (amount <= 0 || amount > 999999999999L) throw new IllegalArgumentException("금액은 1~999999999999원이어야 합니다.");
        if (memo != null && memo.length() > 2000) throw new IllegalArgumentException("메모는 2000자 이하여야 합니다.");
        if (participantIds == null || participantIds.isEmpty()
                || participantIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("지출 참여자가 필요합니다.");
        }
        this.meetingId = meetingId;
        this.payerMemberId = payerMemberId;
        this.title = title.strip();
        this.amount = amount;
        this.memo = memo;
        this.participantIds.addAll(participantIds);
    }

    public Set<Long> getParticipantIds() { return Set.copyOf(participantIds); }

    public void attachReceipt(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || storageKey.length() > 500) {
            throw new IllegalArgumentException("영수증 저장 경로를 확인해주세요.");
        }
        if (receiptKey != null) throw new IllegalStateException("영수증은 한 장만 첨부할 수 있습니다.");
        receiptKey = storageKey;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
