package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

import java.time.LocalDate;

public class DuplicateScheduleCandidateException extends BusinessException {
    public DuplicateScheduleCandidateException(LocalDate candidateDate) {
        super(
                409,
                "이미 있는 일정 후보입니다. = "
                + candidateDate
        );

    }
}
