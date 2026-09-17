package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingAccessDeniedException extends BusinessException {

    public MeetingAccessDeniedException(Long meetingId, Long userId) {

        super(
            403,
            "모임에 대한 권한이 없습니다. meetingId = " + meetingId + ", userId = " + userId
        );
    }
}
