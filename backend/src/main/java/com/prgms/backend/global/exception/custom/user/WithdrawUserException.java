package com.prgms.backend.global.exception.custom.user;

import com.prgms.backend.global.exception.BusinessException;

public class WithdrawUserException extends BusinessException {
    public WithdrawUserException() {
        super(401, "탈퇴한 회원입니다.");
    }
}
