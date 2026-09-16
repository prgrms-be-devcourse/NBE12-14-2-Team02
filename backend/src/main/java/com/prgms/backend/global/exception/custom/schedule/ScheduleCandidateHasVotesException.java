package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

public class ScheduleCandidateHasVotesException extends BusinessException {
    public ScheduleCandidateHasVotesException(Long candidateId) {
        super(
                409,
                "투표가 있는 후보는 수정하거나 삭제할 수 없습니다. id = " + candidateId
        );
    }
}
