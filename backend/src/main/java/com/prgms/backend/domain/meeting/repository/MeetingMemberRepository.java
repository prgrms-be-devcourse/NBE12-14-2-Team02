package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MeetingMemberRepository
    extends JpaRepository<MeetingMember, Long> {

    Optional<MeetingMember> findByMeetingIdAndUserId(
        Long meetingId,
        Long userId
    );

    boolean existsByMeetingIdAndUserId(
        Long meetingId,
        Long userId
    );

    List<MeetingMember> findAllByMeetingId(Long meetingId);
}