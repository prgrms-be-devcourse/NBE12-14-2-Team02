package com.prgms.backend.global.exception.custom.meeting;

import com.prgms.backend.global.exception.BusinessException;

public class MeetingNotFoundException extends BusinessException {
    public MeetingNotFoundException(Long id){
        super(404,"모임이 존재하지 않습니다. id = " + id);
    }
}
