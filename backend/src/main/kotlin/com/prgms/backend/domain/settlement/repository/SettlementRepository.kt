package com.prgms.backend.domain.settlement.repository

import com.prgms.backend.domain.settlement.entity.Settlement
import com.prgms.backend.domain.settlement.entity.SettlementStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SettlementRepository : JpaRepository<Settlement, Long> {
    fun findByMeetingId(meetingId: Long): Optional<Settlement>
    fun existsByMeetingIdAndStatus(meetingId: Long, status: SettlementStatus): Boolean
}
