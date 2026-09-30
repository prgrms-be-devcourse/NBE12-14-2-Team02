package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingHostCannotLeaveException extends BusinessException {

    public MeetingHostCannotLeaveException(Long meetingId, Long userId) {

        super(409, "진행 중인 모임의 모임장은 탈퇴할 수 없습니다.");
    }
}
