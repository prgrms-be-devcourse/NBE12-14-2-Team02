package com.prgms.backend.global.exception.custom;

import com.prgms.backend.global.exception.BusinessException;

public class SchedulePollClosedException extends BusinessException {
    public SchedulePollClosedException() {
        super(
                400,
                "이미 마감된 투표입니다."
        );
    }
}
