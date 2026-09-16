package com.prgms.backend.domain.schedule.vote.repository;

import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScheduleVoteRepository extends JpaRepository<ScheduleVote,Long> {
    boolean existsByScheduleCandidateId(Long candidateId);
}
