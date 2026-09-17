package com.prgms.backend.domain.user.exception;

import com.prgms.backend.global.exception.BusinessException;

public class PasswordMismatchException extends BusinessException {

    public PasswordMismatchException() {
        super(400, "비밀번호가 일치하지 않습니다.");
    }
}
