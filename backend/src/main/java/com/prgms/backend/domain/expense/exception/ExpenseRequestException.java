package com.prgms.backend.domain.expense.exception;

import com.prgms.backend.global.exception.BusinessException;

public class ExpenseRequestException extends BusinessException {
    public ExpenseRequestException(int code, String message) {
        super(code, message);
    }
}
