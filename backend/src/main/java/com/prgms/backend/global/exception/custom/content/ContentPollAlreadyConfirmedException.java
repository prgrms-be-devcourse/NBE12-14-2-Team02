package com.prgms.backend.global.exception.custom.content;

import com.prgms.backend.global.exception.BusinessException;

public class ContentPollAlreadyConfirmedException extends BusinessException {
    public ContentPollAlreadyConfirmedException() {
        super(409,"이미 확정된 콘텐츠가 있습니다.");
    }
}
