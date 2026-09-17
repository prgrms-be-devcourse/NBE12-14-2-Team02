package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingInvitationNotFoundException extends BusinessException {

  public MeetingInvitationNotFoundException(String inviteCode) {
    super(
        404,
        "유효하지 않은 초대 코드입니다. inviteCode = " + inviteCode
    );
  }
}
