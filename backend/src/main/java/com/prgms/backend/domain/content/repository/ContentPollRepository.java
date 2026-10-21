package com.prgms.backend.domain.content.repository;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ContentPollRepository extends JpaRepository<ContentPoll,Long> {
    Optional<ContentPoll> findByMeetingId(Long meetingId);

    // 이 모임에 투표판이 존제는지에 대한 함수
    boolean existsByMeetingId(Long meetingId);

    // ACTIVE Meeting만 조회 -> 종료/삭제된 모임의 OPEN 투표판이 매 스케줄링마다 조회되는 것 방지
    @Query("""
        SELECT cp.id
        FROM ContentPoll cp
        JOIN Meeting m ON m.id = cp.meetingId
        WHERE cp.status = :pollStatus
          AND cp.deadline <= :now
          AND m.status = :meetingStatus
          AND m.deletedAt IS NULL
        """)
    List<Long> findExpiredIds(
            @Param("pollStatus") ContentPollStatus pollStatus,
            @Param("meetingStatus") MeetingStatus meetingStatus,
            @Param("now") LocalDateTime now
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT cp
        FROM ContentPoll cp
        WHERE cp.id = :pollId
        """)
    Optional<ContentPoll> findByIdForUpdate(@Param("pollId") Long pollId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT cp
        FROM ContentPoll cp
        WHERE cp.meetingId = :meetingId
        """)
    Optional<ContentPoll> findByMeetingIdForUpdate(@Param("meetingId") Long meetingId);

    @Query("""
        SELECT cp.meetingId
        FROM ContentPoll cp
        WHERE cp.id = :pollId
        """)
    Optional<Long> findMeetingIdByPollId(@Param("pollId") Long pollId);

    boolean existsByMeetingIdAndStatus(
        Long meetingId,
        ContentPollStatus status
    );
}
