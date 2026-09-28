package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeetingMemberRepository
    extends JpaRepository<MeetingMember, Long> {

    // MeetingMemberStatus 상관없이 회원 기록 조회
    Optional<MeetingMember> findByMeetingIdAndUserId(
        Long meetingId,
        Long userId
    );

    // 모임원이 현재 실제로 참여 중인지 검사(status = JOINED)
    boolean existsByMeetingIdAndUserIdAndStatus(
        Long meetingId,
        Long userId,
        MeetingMemberStatus status
    );

    // 현재 모임원 목록 조회(status = JOINED인 모임원)
    @EntityGraph(attributePaths = "user")
    List<MeetingMember> findAllByMeetingIdAndStatus(
        Long meetingId,
        MeetingMemberStatus status
    );

    // 해당 회원이 JOINED 상태로 참여 중인 모임 목록 조회
    List<MeetingMember> findAllByUserIdAndStatus(
        Long userId,
        MeetingMemberStatus status
    );

    // 삭제(soft delete)된 모임인지 검사
    @EntityGraph(attributePaths = {"meeting", "meeting.host"})
    List<MeetingMember> findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
        Long userId,
        MeetingMemberStatus status
    );

    // 모임원 수
    long countByMeetingIdAndStatus(
        Long meetingId,
        MeetingMemberStatus status
    );

    @Query("""
    SELECT mm.meeting.id, COUNT(mm)
    FROM MeetingMember mm
    WHERE mm.meeting.id IN :meetingIds
      AND mm.status = :status
    GROUP BY mm.meeting.id
    """)
    List<Object[]> countByMeetingIdsAndStatus(
        @Param("meetingIds") List<Long> meetingIds,
        @Param("status") MeetingMemberStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT mm
    FROM MeetingMember mm
    WHERE mm.meeting.id = :meetingId
      AND mm.user.id = :userId
    """)
    Optional<MeetingMember>
    findByMeetingIdAndUserIdForUpdate(
        @Param("meetingId") Long meetingId,
        @Param("userId") Long userId
    );

    @EntityGraph(attributePaths = "user")
    @Query("""
    SELECT mm
    FROM MeetingMember mm
    WHERE mm.meeting.id = :meetingId
      AND mm.joinedAt <= :atTime
      AND (
          mm.leftAt IS NULL
          OR mm.leftAt > :atTime
      )
    """)
    List<MeetingMember> findParticipantsAt(
        @Param("meetingId") Long meetingId,
        @Param("atTime") LocalDateTime atTime
    );
}