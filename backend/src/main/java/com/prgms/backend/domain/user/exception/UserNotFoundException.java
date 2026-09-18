package com.prgms.backend.domain.user.exception;

import com.prgms.backend.global.exception.BusinessException;

public class UserNotFoundException extends BusinessException {
    public UserNotFoundException (String message) {
        super(404, message);
    }
}
