package com.prgms.backend.domain.expense.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "expense_receipts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseReceipt {
    @Id
    private Long id;
    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id")
    private Expense expense;
    @Lob
    @Column(nullable = false)
    private byte[] content;

    public ExpenseReceipt(Expense expense, byte[] content) {
        this.expense = expense;
        this.content = content;
    }
}
