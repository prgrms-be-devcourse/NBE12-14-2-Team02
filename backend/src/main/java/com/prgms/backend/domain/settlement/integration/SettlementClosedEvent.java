package com.prgms.backend.domain.settlement.integration;

import java.time.LocalDateTime;

//정산 마감, 추후 진행
public record SettlementClosedEvent(long meetingId, long settlementId, LocalDateTime closedAt) {}
