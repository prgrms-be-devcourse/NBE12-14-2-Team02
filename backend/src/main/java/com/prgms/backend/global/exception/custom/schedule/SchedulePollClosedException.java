package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

public class SchedulePollClosedException extends BusinessException {
    public SchedulePollClosedException() {
        super(
                409,
                "이미 마감된 일정 투표입니다."
        );
    }
}
