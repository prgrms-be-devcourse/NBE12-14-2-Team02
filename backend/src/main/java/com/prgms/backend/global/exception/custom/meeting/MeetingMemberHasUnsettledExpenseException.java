package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingMemberHasUnsettledExpenseException extends BusinessException {

    public MeetingMemberHasUnsettledExpenseException(
        Long meetingId,
        Long userId
    ) {
        super(409, "미정산 지출에 포함된 모임원은 탈퇴할 수 없습니다.");
    }
}
