package com.prgms.backend.domain.schedule.poll.repository;

import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {
    boolean existsByMeetingId(Long meetingId);
    Optional<SchedulePoll> findByMeetingId(Long meetingId);

    @Query("""
        SELECT DISTINCT sp
        FROM SchedulePoll sp
        LEFT JOIN FETCH sp.candidates
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdWithCandidates(
            @Param("meetingId") Long meetingId
    );
    //조회 시 락 획득
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sp
        FROM SchedulePoll sp
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdForUpdate(
            @Param("meetingId") Long meetingId
    );

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("""
        SELECT sp
        FROM SchedulePoll sp
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdForShare(
            @Param("meetingId") Long meetingId
    );

    @Query("""
    SELECT sp.id
    FROM SchedulePoll sp
    JOIN sp.meeting m
    WHERE sp.status = :pollStatus
      AND sp.deadline <= :now
      AND m.status = :meetingStatus
      AND m.deletedAt IS NULL
    """)
    List<Long> findExpiredIds(
        @Param("pollStatus") SchedulePollStatus pollStatus,
        @Param("meetingStatus") MeetingStatus meetingStatus,
        @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT sp
            FROM SchedulePoll sp
            WHERE sp.id = :pollId
            """)
    Optional<SchedulePoll> findByIdForUpdate(
            @Param("pollId") Long pollId
    );

    @Query("""
    SELECT sp.meeting.id
    FROM SchedulePoll sp
    WHERE sp.id = :pollId
    """)
    Optional<Long> findMeetingIdByPollId(
        @Param("pollId") Long pollId
    );
}
