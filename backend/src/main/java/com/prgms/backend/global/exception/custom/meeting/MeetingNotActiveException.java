package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingNotActiveException extends BusinessException {

    public MeetingNotActiveException(Long meetingId) {
        super(409, "진행 중인 모임이 아닙니다. meetingId=" + meetingId);
    }
}