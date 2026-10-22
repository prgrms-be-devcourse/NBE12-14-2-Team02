package com.prgms.backend.domain.settlement.integration

import com.prgms.backend.domain.meeting.entity.Meeting
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository
import com.prgms.backend.domain.user.entity.SecurityUser
import com.prgms.backend.global.exception.custom.settlement.SettlementRequestException
import jakarta.persistence.EntityManager
import jakarta.persistence.LockModeType
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.security.Principal

@Component
class MeetingAccessAdapter(private val entityManager: EntityManager,
                           private val memberRepository: MeetingMemberRepository) : MeetingAccessPort {
    override fun requireMember(meetingId: Long, principal: Principal?) = requireMember(meetingId, principal, true)
    override fun requireMemberForRead(meetingId: Long, principal: Principal?) = requireMember(meetingId, principal, false)

    private fun requireMember(meetingId: Long, principal: Principal?, lock: Boolean): MeetingAccessPort.Context {
        val authentication = principal as? Authentication
        val loginUser = authentication?.principal as? SecurityUser
        if (authentication?.isAuthenticated != true || loginUser == null)
            throw SettlementRequestException(401, "로그인이 필요합니다.")
        val userId = loginUser.id
        // 아직 정산 행이 없어도 모임 행을 잠가 최초 확정 요청을 직렬화합니다.
        val meeting = if (lock) entityManager.find(Meeting::class.java, meetingId, LockModeType.PESSIMISTIC_WRITE)
                      else entityManager.find(Meeting::class.java, meetingId)
        if (meeting == null || meeting.isDeleted) throw SettlementRequestException(404, "모임을 찾을 수 없습니다.")
        val member = memberRepository.findByMeetingIdAndUserId(meetingId, userId).filter { it.isJoined }
            .orElseThrow { SettlementRequestException(403, "모임 참여자만 이용할 수 있습니다.") }
        // 과거 탈퇴자도 기존 지출의 결제자/부담자일 수 있습니다.
        val memberIds = entityManager.createQuery(
            "select m.id from MeetingMember m where m.meeting.id = :meetingId", Long::class.javaObjectType)
            .setParameter("meetingId", meetingId).resultList.toSet()
        return MeetingAccessPort.Context(member.id, meeting.isHost(userId), meeting.isActive, memberIds)
    }
}
