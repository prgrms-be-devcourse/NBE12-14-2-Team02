package com.prgms.backend.domain.user.exception;

import com.prgms.backend.global.exception.BusinessException;

public class DuplicateEmailNickname extends BusinessException {

    public DuplicateEmailNickname() {
        super(409, "이미 존재하는 이메일 또는 닉네임이 사용되었습니다.");
    }
}
