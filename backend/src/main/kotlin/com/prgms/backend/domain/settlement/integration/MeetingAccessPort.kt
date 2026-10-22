package com.prgms.backend.domain.settlement.integration

import java.security.Principal

interface MeetingAccessPort {
    fun requireMember(meetingId: Long, principal: Principal?): Context
    fun requireMemberForRead(meetingId: Long, principal: Principal?): Context

    class Context(val memberId: Long, val leader: Boolean, val meetingOpen: Boolean, memberIds: Set<Long>) {
        val memberIds: Set<Long> = java.util.Set.copyOf(memberIds)
        // 기존 Java 호출부의 record 접근자를 유지합니다.
        fun memberId() = memberId
        fun leader() = leader
        fun meetingOpen() = meetingOpen
        fun memberIds() = memberIds
    }
}
