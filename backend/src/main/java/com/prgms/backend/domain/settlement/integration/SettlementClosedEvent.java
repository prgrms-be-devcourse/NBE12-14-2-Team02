package com.prgms.backend.domain.settlement.integration;

import java.time.LocalDateTime;

// 정산 트랜잭션이 커밋된 뒤 알림을 저장하기 위한 이벤트
public record SettlementClosedEvent(long meetingId, long settlementId, LocalDateTime closedAt) {}
