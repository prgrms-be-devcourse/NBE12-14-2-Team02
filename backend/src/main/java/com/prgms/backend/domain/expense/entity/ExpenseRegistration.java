package com.prgms.backend.domain.expense.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;

// 지출 삭제 후에도 요청 번호를 보존해 늦게 도착한 재요청이 지출을 복원하지 않게 합니다.
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "expense_registrations", uniqueConstraints = @UniqueConstraint(
        name = "uk_expense_registration", columnNames = {"meeting_id", "member_id", "request_key"}))
public class ExpenseRegistration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "meeting_id", nullable = false) private long meetingId;
    @Column(name = "member_id", nullable = false) private long memberId;
    @Column(name = "request_key", nullable = false, length = 36) private String requestKey;
    @Column(nullable = false, length = 64) private String fingerprint;
    @Column(nullable = false) private long expenseId;

    public ExpenseRegistration(long meetingId, long memberId, String requestKey, String fingerprint, long expenseId) {
        this.meetingId = meetingId;
        this.memberId = memberId;
        this.requestKey = requestKey;
        this.fingerprint = fingerprint;
        this.expenseId = expenseId;
    }
}
