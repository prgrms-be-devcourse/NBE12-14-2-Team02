package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingHostRequiredException
        extends BusinessException {

    public MeetingHostRequiredException() {
        super(403, "모임장만 수행할 수 있습니다.");
    }
}