package com.prgms.backend.domain.settlement.integration;

import java.security.Principal;
import java.util.Set;

// 로그인 사용자와 모임 정보 연결
public interface MeetingAccessPort {
    Context requireMember(long meetingId, Principal principal);
    Context requireMemberForRead(long meetingId, Principal principal);

    // 과거에 탈퇴헌 멤버도 포함
    record Context(long memberId, boolean leader, boolean meetingOpen, Set<Long> memberIds) {
        public Context {
            memberIds = Set.copyOf(memberIds);
        }
    }
}
