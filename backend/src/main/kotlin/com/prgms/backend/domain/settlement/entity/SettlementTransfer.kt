package com.prgms.backend.domain.settlement.entity

import jakarta.persistence.Embeddable

@Embeddable
class SettlementTransfer(val senderId: Long, val recipientId: Long, val amount: Long)
