package com.prgms.backend.global.exception.custom;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingMemberNotFoundException extends BusinessException {

    public MeetingMemberNotFoundException(Long meetingMemberId) {
        super(
            404,
            "모임 멤버가 존재하지 않습니다. meetingMemberId = "
                + meetingMemberId
        );
    }

    public MeetingMemberNotFoundException(Long meetingId, Long userId) {

        super(
            404,
            "해당 모임에 사용자가 존재하지 않습니다. meetingId = " + meetingId + ", userId = " + userId
        );
    }
}
