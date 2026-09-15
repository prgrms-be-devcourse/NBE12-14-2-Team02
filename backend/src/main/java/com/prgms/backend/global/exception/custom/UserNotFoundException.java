package com.prgms.backend.global.exception.custom;

import com.prgms.backend.global.exception.BusinessException;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(Long id) {

        super(404, "사용자가 존재하지 않습니다. id = " + id);
    }
}
