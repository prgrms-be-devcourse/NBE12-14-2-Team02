package com.prgms.backend.domain.content.integration;

import com.prgms.backend.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentNotificationWriter {
    private final NotificationService notificationService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(long meetingId){
        notificationService.notifyContentClosed(meetingId);
    }
}
