package com.prgms.backend.domain.expense.repository;

import com.prgms.backend.domain.expense.entity.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByMeetingIdOrderByIdAsc(long meetingId);
}
