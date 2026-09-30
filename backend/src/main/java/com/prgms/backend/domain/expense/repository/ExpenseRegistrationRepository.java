package com.prgms.backend.domain.expense.repository;

import com.prgms.backend.domain.expense.entity.ExpenseRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ExpenseRegistrationRepository extends JpaRepository<ExpenseRegistration, Long> {
    Optional<ExpenseRegistration> findByMeetingIdAndMemberIdAndRequestKey(long meetingId, long memberId, String requestKey);
}
