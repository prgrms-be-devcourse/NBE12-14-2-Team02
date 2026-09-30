package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingMemberAlreadyLeftException extends BusinessException {

    public MeetingMemberAlreadyLeftException(Long meetingId, Long userId) {

        super(
            409,
            "이미 탈퇴한 모임원입니다. meetingId = "
                + meetingId + ", userId = " + userId
        );
    }
}
