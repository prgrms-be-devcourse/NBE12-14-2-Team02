package com.prgms.backend.domain.expense.entity

import jakarta.persistence.*

@Entity
@Table(name = "expense_registrations", uniqueConstraints = [UniqueConstraint(
    name = "uk_expense_registration", columnNames = ["meeting_id", "member_id", "request_key"])])
class ExpenseRegistration(
    @field:Column(name = "meeting_id", nullable = false) val meetingId: Long,
    @field:Column(name = "member_id", nullable = false) val memberId: Long,
    @field:Column(name = "request_key", nullable = false, length = 36) val requestKey: String,
    @field:Column(nullable = false, length = 64) val fingerprint: String,
    @field:Column(nullable = false) val expenseId: Long
) {
    @field:Id @field:GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null
        protected set
}
