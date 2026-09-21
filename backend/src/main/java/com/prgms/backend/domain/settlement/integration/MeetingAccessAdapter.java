package com.prgms.backend.domain.settlement.integration;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import org.springframework.security.core.Authentication;
import com.prgms.backend.domain.user.entity.SecurityUser;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class MeetingAccessAdapter implements MeetingAccessPort {
    private final EntityManager entityManager;
    private final MeetingMemberRepository memberRepository;


    @Override
    public Context requireMember(long meetingId, Principal principal) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUser)) {
            throw new SettlementRequestException(401, "로그인이 필요합니다.");
        }
        // 로그인해서 모임에 참여한 사람인지 검증
        SecurityUser loginUser = (SecurityUser) authentication.getPrincipal();
        long userId = loginUser.getId();

        // 아직 정산 데이터가 없어도 존재하는 모임 행을 잠가 최초 확정 요청까지 보호합니다.
        Meeting meeting = entityManager.find(Meeting.class, meetingId, LockModeType.PESSIMISTIC_WRITE);
        if (meeting == null) {
            throw new SettlementRequestException(404, "모임을 찾을 수 없습니다.");
        }
        MeetingMember member = memberRepository.findByMeetingIdAndUserId(meetingId, userId)
                .filter(MeetingMember::isJoined)
                .orElseThrow(() -> new SettlementRequestException(403, "모임 참여자만 이용할 수 있습니다."));

        // 탈퇴한 사람도 과거 지출의 결제자나 부담자일 경우 정산 대상에는 포함
        Set<Long> memberIds = new HashSet<>(entityManager.createQuery(
                "select m.id from MeetingMember m where m.meeting.id = :meetingId", Long.class)
                .setParameter("meetingId", meetingId).getResultList());
        return new Context(member.getId(), meeting.isHost(userId), true, memberIds);
    }
}
