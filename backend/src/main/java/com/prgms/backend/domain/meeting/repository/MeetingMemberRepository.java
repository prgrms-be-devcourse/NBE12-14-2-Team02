package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
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
}