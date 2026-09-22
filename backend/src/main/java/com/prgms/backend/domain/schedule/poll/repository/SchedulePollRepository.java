package com.prgms.backend.domain.schedule.poll.repository;

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

    List<SchedulePoll> findByStatusAndDeadlineLessThanEqual(SchedulePollStatus schedulePollStatus, LocalDateTime now);

    //FETCH JOIN으로 SchedulePoll을 불러올때, candidates도 함께 불러올 수 있도록.
    @Query("""
        SELECT DISTINCT sp
        FROM SchedulePoll sp
        LEFT JOIN FETCH sp.candidates
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdWithCandidates(
            @Param("meetingId") Long meetingId
    );

    //SchedulePoll조회 시 meeting까지 불러올 수 있도록.
    @Query("""
        SELECT sp
        FROM SchedulePoll sp
        JOIN FETCH sp.meeting
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdWithMeeting(
            @Param("meetingId") Long meetingId
    );

    //meeting과 ScheduleCandidate까지 불러올 수 있도록.
    @Query("""
        SELECT DISTINCT sp
        FROM SchedulePoll sp
        JOIN FETCH sp.meeting
        LEFT JOIN FETCH sp.candidates
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdWithMeetingAndCandidates(
            @Param("meetingId") Long meetingId
    );


    //조회 시 락을 적용해 다른 요청이 조회할 수 없도록 제어함.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT sp
        FROM SchedulePoll sp
        JOIN FETCH sp.meeting
        WHERE sp.meeting.id = :meetingId
        """)
    Optional<SchedulePoll> findByMeetingIdWithMeetingForUpdate(
            @Param("meetingId") Long meetingId
    );
}
