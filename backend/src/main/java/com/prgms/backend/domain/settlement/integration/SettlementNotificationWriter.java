package com.prgms.backend.domain.settlement.integration;

import com.prgms.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SettlementNotificationWriter {
    private final NotificationService notifications;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(long meetingId) {
        notifications.notifySettlementClosed(meetingId);
    }
}
