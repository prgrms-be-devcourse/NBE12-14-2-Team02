package com.prgms.backend.domain.user.exception;

import com.prgms.backend.global.exception.BusinessException;

public class PasswordMissmatchException extends BusinessException {

    public PasswordMissmatchException() {
        super(400, "비밀번호가 일치하지 않습니다.");
    }
}
