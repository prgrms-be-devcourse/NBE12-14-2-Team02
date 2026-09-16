package com.prgms.backend.domain.schedule.candidate.repository;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleCandidateRepository extends JpaRepository<ScheduleCandidate, Long> {
}
