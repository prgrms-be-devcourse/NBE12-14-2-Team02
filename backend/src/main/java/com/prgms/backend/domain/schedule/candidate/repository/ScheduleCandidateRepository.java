package com.prgms.backend.domain.schedule.candidate.repository;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface ScheduleCandidateRepository extends JpaRepository<ScheduleCandidate, Long> {
    boolean existsBySchedulePollIdAndCandidateDate(
            Long schedulePollId,
            LocalDate candidateDate
    );
}
