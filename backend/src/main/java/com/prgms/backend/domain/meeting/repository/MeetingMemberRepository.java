package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingMemberRepository
    extends JpaRepository<MeetingMember, Long> {

    // MeetingMemberStatus 상관없이 회원 기록 찾기
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

    // 현재 모임원 목록 찾기(status = JOINED인 모임원)
    List<MeetingMember> findAllByMeetingIdAndStatus(
        Long meetingId,
        MeetingMemberStatus status
    );
}