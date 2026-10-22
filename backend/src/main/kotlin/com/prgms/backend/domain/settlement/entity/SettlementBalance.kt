package com.prgms.backend.domain.settlement.entity

import jakarta.persistence.Embeddable

@Embeddable
class SettlementBalance(val memberId: Long, val paidAmount: Long, val shareAmount: Long)
