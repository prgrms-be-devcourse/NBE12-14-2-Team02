package com.prgms.backend.domain.schedule.candidate.repository;

import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ScheduleCandidateRepository extends JpaRepository<ScheduleCandidate, Long> {

    @Query("""
        SELECT candidate
        FROM ScheduleCandidate candidate
        JOIN FETCH candidate.schedulePoll poll
        WHERE candidate.id = :candidateId
          AND poll.meeting.id = :meetingId
        """)
    Optional<ScheduleCandidate> findByIdAndMeetingIdWithSchedulePoll(
            @Param("candidateId") Long candidateId,
            @Param("meetingId") Long meetingId
    );
}
