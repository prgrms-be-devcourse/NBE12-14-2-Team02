package com.prgms.backend.global.exception.custom;

import com.prgms.backend.global.exception.BusinessException;

public class SchedulePollAlreadyExistsException extends BusinessException {
    public SchedulePollAlreadyExistsException(Long meetingId) {
        super(
                409,
                "이미 일정 투표가 존재합니다. meetingId = "
                + meetingId
        );

    }
}
