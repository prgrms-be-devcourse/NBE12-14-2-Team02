package com.prgms.backend.domain.schedule.vote.repository;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScheduleVoteRepository extends JpaRepository<ScheduleVote,Long> {
    boolean existsByScheduleCandidateId(Long candidateId);

    Optional<ScheduleVote> findByScheduleCandidateIdAndMeetingMemberId(Long candidateId, Long meetingMemberId);

    List<ScheduleVote> findAllByScheduleCandidateSchedulePollId(Long schedulePollId);

    @Modifying
    @Query("""
    DELETE FROM ScheduleVote sv
    WHERE sv.meetingMember.id = :meetingMemberId
      AND sv.scheduleCandidate.id IN (
          SELECT sc.id
          FROM ScheduleCandidate sc
          JOIN sc.schedulePoll sp
          WHERE sp.meeting.id = :meetingId
            AND sp.status = :status
            AND sp.deadline > :now
      )
    """)
    int deleteOpenVotesByMeetingMember(
        @Param("meetingId") Long meetingId,
        @Param("meetingMemberId") Long meetingMemberId,
        @Param("status") SchedulePollStatus status,
        @Param("now") LocalDateTime now
    );
}
