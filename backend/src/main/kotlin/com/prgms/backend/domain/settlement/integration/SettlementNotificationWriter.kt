package com.prgms.backend.domain.settlement.integration

import com.prgms.backend.domain.notification.service.NotificationService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class SettlementNotificationWriter(private val notifications: NotificationService) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun save(meetingId: Long) { notifications.notifySettlementClosed(meetingId) }
}
