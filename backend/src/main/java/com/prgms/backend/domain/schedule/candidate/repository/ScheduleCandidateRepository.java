package com.prgms.backend.domain.schedule.candidate.repository;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleCandidateRepository extends JpaRepository<ScheduleCandidate, Long> {

    Optional<ScheduleCandidate> findByIdAndSchedulePollId(Long id, Long schedulePollId);
}
