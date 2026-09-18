package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MeetingInvitationRepository extends JpaRepository<MeetingInvitation, Long> {

    // inviteCode로 초대 찾기
    Optional<MeetingInvitation> findByInviteCode(String inviteCode);

    // 존재하는 초대인지
    boolean existsByInviteCode(String inviteCode);

    // 초대 코드 목록 조회
    List<MeetingInvitation> findAllByMeetingId(Long meetingId);
}
