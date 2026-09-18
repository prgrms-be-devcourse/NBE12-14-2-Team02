package com.prgms.backend.domain.schedule.vote.repository;

import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScheduleVoteRepository extends JpaRepository<ScheduleVote,Long> {
    boolean existsByScheduleCandidateId(Long candidateId);

    Optional<ScheduleVote> findByScheduleCandidateIdAndMeetingMemberId(Long candidateId, Long meetingMemberId);
}
