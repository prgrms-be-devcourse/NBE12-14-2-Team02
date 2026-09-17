package com.prgms.backend.domain.notification.dto.response;

import com.prgms.backend.domain.notification.ENUM.NotificationType;
import com.prgms.backend.domain.notification.entity.Notification;

import java.time.LocalDateTime;

public record NotificationResponse(
        Long id,
        Long meetingId,
        NotificationType type,
        String title,
        String content,
        String redirectUrl,
        boolean isRead,
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getMeetingId(),
                notification.getType(),
                notification.getTitle(),
                notification.getContent(),
                notification.getRedirectUrl(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
