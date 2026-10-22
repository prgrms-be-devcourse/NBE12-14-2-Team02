package com.prgms.backend.domain.settlement.service

import com.prgms.backend.domain.meeting.entity.MeetingMember
import com.prgms.backend.domain.settlement.dto.SettlementResponse
import com.prgms.backend.domain.settlement.entity.Settlement
import com.prgms.backend.domain.settlement.entity.SettlementStatus
import com.prgms.backend.domain.user.entity.SecurityUser
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException
import jakarta.persistence.EntityManager
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Principal

@Service
class SettlementHistoryService(private val entityManager: EntityManager) {
    @JvmRecord data class Entry(val meetingName: String, val currentMemberId: Long,
                               val names: Map<Long, String>, val result: SettlementResponse)

    @Transactional(readOnly = true)
    fun getMine(principal: Principal?): List<Entry> {
        val authentication = principal as? Authentication
        val user = authentication?.principal as? SecurityUser
        if (authentication?.isAuthenticated != true || user == null)
            throw SettlementRequestException(401, "로그인이 필요합니다.")
        val memberships = entityManager.createQuery("""
            select m from MeetingMember m join fetch m.meeting
            where m.user.id = :userId and m.meeting.deletedAt is null
        """, MeetingMember::class.java).setParameter("userId", user.id).resultList
        if (memberships.isEmpty()) return emptyList()
        val byMeeting = memberships.associateBy { it.meeting.id }
        val settlements = entityManager.createQuery("""
            select distinct s from Settlement s left join fetch s.balances
            where s.meetingId in :ids and s.status = :status order by s.closedAt desc
        """, Settlement::class.java).setParameter("ids", byMeeting.keys)
            .setParameter("status", SettlementStatus.CLOSED).resultList
        if (settlements.isEmpty()) return emptyList()
        val ids = settlements.map { it.id }
        // 여러 컬렉션의 곱집합을 피하고 컬렉션별로 일괄 조회합니다.
        entityManager.createQuery("select distinct s from Settlement s left join fetch s.transfers where s.id in :ids", Settlement::class.java)
            .setParameter("ids", ids).resultList
        entityManager.createQuery("select distinct s from Settlement s left join fetch s.accounts where s.id in :ids", Settlement::class.java)
            .setParameter("ids", ids).resultList
        val entries = mutableListOf<Entry>()
        val visibleMembers = hashSetOf<Long>()
        for (settlement in settlements) {
            val member = byMeeting.getValue(settlement.meetingId)
            val personal = SettlementResponse.forMember(settlement, member.id, true)
            if (personal.balances.isEmpty()) continue
            personal.transfers.forEach { visibleMembers.add(it.senderId); visibleMembers.add(it.recipientId) }
            entries.add(Entry(member.meeting.name, member.id, emptyMap(), personal))
        }
        val names = if (visibleMembers.isEmpty()) emptyMap() else entityManager.createQuery(
            "select m from MeetingMember m join fetch m.user where m.id in :ids", MeetingMember::class.java)
            .setParameter("ids", visibleMembers).resultList.associate { it.id to it.user.nickname }
        return entries.map { entry ->
            val relevantNames = hashMapOf<Long, String>()
            entry.result.transfers.forEach {
                relevantNames[it.senderId] = names[it.senderId] ?: "모임원 #${it.senderId}"
                relevantNames[it.recipientId] = names[it.recipientId] ?: "모임원 #${it.recipientId}"
            }
            Entry(entry.meetingName, entry.currentMemberId, relevantNames, entry.result)
        }
    }
}
