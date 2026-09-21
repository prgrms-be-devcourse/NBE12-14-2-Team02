package com.prgms.backend.global.exception.custom.settlement;

import com.prgms.backend.global.exception.BusinessException;

public class SettlementRequestException extends BusinessException {
    public SettlementRequestException(int status, String message) {
        super(status, message);
    }
}
