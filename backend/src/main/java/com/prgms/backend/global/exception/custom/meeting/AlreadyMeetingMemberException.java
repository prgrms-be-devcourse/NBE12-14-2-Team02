package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class AlreadyMeetingMemberException extends BusinessException {

    public AlreadyMeetingMemberException(Long meetingId, Long userId) {
        super(
            409,
            "이미 모임에 참여 중인 사용자입니다. meetingId = "
                + meetingId + ", userId = " + userId
        );
    }
}
