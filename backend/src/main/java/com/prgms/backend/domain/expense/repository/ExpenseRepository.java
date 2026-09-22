package com.prgms.backend.domain.expense.repository;

import com.prgms.backend.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByMeetingIdOrderByIdAsc(long meetingId);

    boolean existsByMeetingId(Long meetingId);

    @Query("""
    SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
    FROM Expense e
    WHERE e.meetingId = :meetingId
      AND (
          e.payerMemberId = :meetingMemberId
          OR :meetingMemberId MEMBER OF e.participantIds
      )
    """)
    boolean existsMemberInExpense(
        @Param("meetingId") Long meetingId,
        @Param("meetingMemberId") Long meetingMemberId
    );
}
