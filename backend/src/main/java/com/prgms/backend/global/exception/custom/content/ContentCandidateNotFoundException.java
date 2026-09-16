package com.prgms.backend.global.exception.custom.content;

import com.prgms.backend.global.exception.BusinessException;

public class ContentCandidateNotFoundException extends BusinessException {
    public ContentCandidateNotFoundException(Long candidateId) {
        super(404, "콘텐츠 후보가 존재하지 않습니다. candidateId = " + candidateId);
    }
}
