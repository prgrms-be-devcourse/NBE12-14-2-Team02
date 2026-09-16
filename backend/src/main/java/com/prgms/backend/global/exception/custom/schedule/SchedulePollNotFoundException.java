package com.prgms.backend.global.exception.custom.schedule;

import com.prgms.backend.global.exception.BusinessException;

public class SchedulePollNotFoundException
        extends BusinessException {

  public SchedulePollNotFoundException(Long meetingId) {
    super(
            404,
            "일정 투표를 찾을 수 없습니다. meetingId = "
                    + meetingId
    );
  }
}