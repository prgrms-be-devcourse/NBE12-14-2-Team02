package com.prgms.backend.domain.schedule.repository;

import com.prgms.backend.domain.schedule.entity.ScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleCandidateRepository extends JpaRepository<ScheduleCandidate, Long> {
}
