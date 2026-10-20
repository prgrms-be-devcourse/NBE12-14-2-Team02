package com.prgms.backend.domain.settlement.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementClosedEventListener {
    private final SettlementNotificationWriter notificationWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClosed(SettlementClosedEvent event) {
        try {
            notificationWriter.save(event.meetingId());
        } catch (RuntimeException exception) {
            // 알림 저장/커밋 실패가 이미 확정된 정산의 성공 응답을 바꾸지 않도록 합니다.
            log.error("정산 확정 알림 저장 실패: meetingId={}, settlementId={}",
                    event.meetingId(), event.settlementId(), exception);
        }
    }
}
