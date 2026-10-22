package com.prgms.backend.domain.settlement.integration

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class SettlementClosedEventListener(private val notificationWriter: SettlementNotificationWriter) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onClosed(event: SettlementClosedEvent) {
        try {
            notificationWriter.save(event.meetingId)
        } catch (exception: RuntimeException) {
            // 알림 저장/커밋 실패가 이미 확정된 정산의 성공 응답을 바꾸지 않도록 합니다.
            logger.error("정산 확정 알림 저장 실패: meetingId={}, settlementId={}",
                event.meetingId, event.settlementId, exception)
        }
    }
    companion object {
        private val logger = LoggerFactory.getLogger(SettlementClosedEventListener::class.java)
    }
}
