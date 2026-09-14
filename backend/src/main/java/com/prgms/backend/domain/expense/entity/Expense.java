package com.prgms.backend.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "expenses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Expense {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String payerName;
    @Column(nullable = false, length = 255)
    private String content;
    @Column(nullable = false)
    private long totalCost;
    @Column(nullable = false)
    private LocalDate spentAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(length = 30)
    private String receiptContentType;

    @Column
    private Long meetingId;
    @Column
    private Long payerMemberId;
    @Column(length = 2000)
    private String memo;
    @ElementCollection
    @CollectionTable(name = "expense_participants", joinColumns = @JoinColumn(name = "expense_id"))
    @Column(name = "member_id", nullable = false)
    private java.util.Set<Long> participantIds = new java.util.LinkedHashSet<>();

    public Expense(long meetingId, long payerMemberId, String payerName, String title,
                   long totalCost, LocalDate spentAt, String memo, java.util.Set<Long> participantIds) {
        this(payerName, title, totalCost, spentAt);
        this.meetingId = meetingId;
        this.payerMemberId = payerMemberId;
        this.memo = memo == null ? null : memo.strip();
        this.participantIds.addAll(participantIds);
    }

    public void attachReceipt(String contentType) {
        this.receiptContentType = contentType;
    }

    public Expense(String payerName, String content, long totalCost, LocalDate spentAt) {
        this.payerName = payerName.strip();
        this.content = content.strip();
        this.totalCost = totalCost;
        this.spentAt = spentAt;
    }

    @PrePersist
    private void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
