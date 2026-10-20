package com.prgms.backend.domain.settlement.integration

import java.time.LocalDateTime

@JvmRecord
data class SettlementClosedEvent(val meetingId: Long, val settlementId: Long, val closedAt: LocalDateTime)
