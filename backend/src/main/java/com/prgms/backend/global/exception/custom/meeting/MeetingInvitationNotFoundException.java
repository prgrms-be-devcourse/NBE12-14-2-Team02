package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingInvitationNotFoundException extends BusinessException {

  public MeetingInvitationNotFoundException(String inviteCode) {
    super(
        404,
        "유효하지 않은 초대 코드입니다. inviteCode = " + inviteCode
    );
  }

  public MeetingInvitationNotFoundException(Long invitationId) {
    super(
        404,
        "초대 정보를 찾을 수 없습니다. invitationId=" + invitationId
    );
  }
}
