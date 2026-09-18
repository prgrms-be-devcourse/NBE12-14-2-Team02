package com.prgms.backend.global.exception.custom.notification;

import com.prgms.backend.global.exception.BusinessException;

public class NotificationNotFoundException extends BusinessException {
    public NotificationNotFoundException(Long notificationId){
        super(404,"알림이 존재하지 않습니다. notificationId = "+notificationId);
    }
}
