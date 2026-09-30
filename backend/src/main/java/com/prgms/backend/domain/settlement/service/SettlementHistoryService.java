package com.prgms.backend.domain.settlement.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.settlement.dto.SettlementResponse;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SettlementHistoryService {
    private final EntityManager entityManager;

    public record Entry(String meetingName, long currentMemberId, Map<Long, String> names, SettlementResponse result) {}

    @Transactional(readOnly = true)
    public List<Entry> getMine(Principal principal) {
        if (!(principal instanceof Authentication authentication) || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof SecurityUser user)) {
            throw new SettlementRequestException(401, "로그인이 필요합니다.");
        }
        // 탈퇴 여부와 무관하게 로그인 사용자 자신의 과거 모임 소속을 확인합니다.
        var memberships = entityManager.createQuery("""
                select m from MeetingMember m join fetch m.meeting
                where m.user.id = :userId and m.meeting.deletedAt is null
                """, MeetingMember.class).setParameter("userId", user.getId()).getResultList();
        if (memberships.isEmpty()) return List.of();
        var byMeeting = memberships.stream().collect(Collectors.toMap(m -> m.getMeeting().getId(), m -> m));
        var settlements = entityManager.createQuery("""
                select distinct s from Settlement s left join fetch s.balances
                where s.meetingId in :ids and s.status = :status order by s.closedAt desc
                """, Settlement.class).setParameter("ids", byMeeting.keySet())
                .setParameter("status", SettlementStatus.CLOSED).getResultList();
        if (settlements.isEmpty()) return List.of();
        var ids = settlements.stream().map(Settlement::getId).toList();
        // 여러 컬렉션을 한 번에 곱집합으로 가져오지 않고, 컬렉션별로 일괄 조회합니다.
        entityManager.createQuery("select distinct s from Settlement s left join fetch s.transfers where s.id in :ids", Settlement.class)
                .setParameter("ids", ids).getResultList();
        entityManager.createQuery("select distinct s from Settlement s left join fetch s.accounts where s.id in :ids", Settlement.class)
                .setParameter("ids", ids).getResultList();
        List<Entry> entries = new ArrayList<>();
        Set<Long> visibleMembers = new HashSet<>();
        for (var settlement : settlements) {
            var member = byMeeting.get(settlement.getMeetingId());
            var personal = SettlementResponse.forMember(settlement, member.getId(), true);
            if (personal.balances().isEmpty()) continue;
            personal.transfers().forEach(transfer -> { visibleMembers.add(transfer.senderId()); visibleMembers.add(transfer.recipientId()); });
            entries.add(new Entry(member.getMeeting().getName(), member.getId(), Map.of(), personal));
        }
        Map<Long, String> names = visibleMembers.isEmpty() ? Map.of() : entityManager.createQuery(
                "select m from MeetingMember m join fetch m.user where m.id in :ids", MeetingMember.class)
                .setParameter("ids", visibleMembers).getResultList().stream()
                .collect(Collectors.toMap(MeetingMember::getId, member -> member.getUser().getNickname()));
        return entries.stream().map(entry -> {
            Map<Long, String> relevantNames = new HashMap<>();
            entry.result().transfers().forEach(transfer -> {
                relevantNames.put(transfer.senderId(), names.getOrDefault(transfer.senderId(), "모임원 #" + transfer.senderId()));
                relevantNames.put(transfer.recipientId(), names.getOrDefault(transfer.recipientId(), "모임원 #" + transfer.recipientId()));
            });
            return new Entry(entry.meetingName(), entry.currentMemberId(), relevantNames, entry.result());
        }).toList();
    }
}
