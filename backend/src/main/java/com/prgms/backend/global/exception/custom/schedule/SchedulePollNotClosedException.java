package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

public class SchedulePollNotClosedException
        extends BusinessException {

    public SchedulePollNotClosedException() {
        super(
                409,
                "아직 마감되지 않은 일정 투표입니다."
        );
    }
}