package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

public class ScheduleCandidateLimitExceededException
        extends BusinessException {

    public ScheduleCandidateLimitExceededException(
            int maxCount
    ) {
        super(
                409,
                "일정 후보는 최대 "
                        + maxCount
                        + "개까지 등록할 수 있습니다."
        );
    }
}