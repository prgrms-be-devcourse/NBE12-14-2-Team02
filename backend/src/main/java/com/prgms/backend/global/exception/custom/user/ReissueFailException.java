package com.prgms.backend.global.exception.custom.user;

import com.prgms.backend.global.exception.BusinessException;

public class ReissueFailException extends BusinessException {
    public ReissueFailException() {
        super(401, "reissue에 실패했습니다.");
    }
}
