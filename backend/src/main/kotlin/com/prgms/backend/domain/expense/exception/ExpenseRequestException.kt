package com.prgms.backend.domain.expense.exception

import com.prgms.backend.global.exception.BusinessException

class ExpenseRequestException(code: Int, message: String) : BusinessException(code, message)
