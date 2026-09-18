package com.prgms.backend.domain.user.exception;

import com.prgms.backend.global.exception.BusinessException;

public class LoginFailException extends BusinessException {
    public LoginFailException() {
        super(401, "이메일 또는 비밀번호가 일치하지 않습니다.");
    }
}
