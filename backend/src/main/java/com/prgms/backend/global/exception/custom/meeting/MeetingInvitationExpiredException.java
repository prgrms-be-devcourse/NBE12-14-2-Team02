package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingInvitationExpiredException extends BusinessException {

    public MeetingInvitationExpiredException() {
        super(410, "만료된 초대입니다.");
    }
}
