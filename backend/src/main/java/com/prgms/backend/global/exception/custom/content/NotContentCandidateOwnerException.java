package com.prgms.backend.global.exception.custom.content;

import com.prgms.backend.global.exception.BusinessException;

public class NotContentCandidateOwnerException extends BusinessException {
    public NotContentCandidateOwnerException() {
        super(403, "본인이 등록한 후보만 수정하거나 삭제할 수 있습니다.");
    }
}