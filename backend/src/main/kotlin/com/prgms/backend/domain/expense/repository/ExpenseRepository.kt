package com.prgms.backend.domain.expense.repository

import com.prgms.backend.domain.expense.entity.Expense
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExpenseRepository : JpaRepository<Expense, Long> {
    fun findByMeetingIdOrderByIdAsc(meetingId: Long): List<Expense>
    fun existsByMeetingId(meetingId: Long): Boolean

    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Expense e
        WHERE e.meetingId = :meetingId AND
        (e.payerMemberId = :meetingMemberId OR :meetingMemberId MEMBER OF e.participantIds)
    """)
    fun existsMemberInExpense(@Param("meetingId") meetingId: Long,
                              @Param("meetingMemberId") meetingMemberId: Long): Boolean
}
