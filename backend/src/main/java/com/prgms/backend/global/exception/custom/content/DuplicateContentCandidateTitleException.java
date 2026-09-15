package com.prgms.backend.global.exception.custom.content;

import com.prgms.backend.global.exception.BusinessException;

public class DuplicateContentCandidateTitleException extends BusinessException {
    public DuplicateContentCandidateTitleException(String title) {
        super(409, "같은 투표에 동일한 제목의 후보가 있습니다. title = " + title);
    }
}
