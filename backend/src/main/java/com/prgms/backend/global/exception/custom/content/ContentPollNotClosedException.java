package com.prgms.backend.global.exception.custom.content;

import com.prgms.backend.global.exception.BusinessException;

public class ContentPollNotClosedException extends BusinessException {
    public ContentPollNotClosedException() {
        this("마감되지 않은 투표는 확정할 수 없습니다.");
    }

    public ContentPollNotClosedException(String message) {
        super(409, message);
    }
}
