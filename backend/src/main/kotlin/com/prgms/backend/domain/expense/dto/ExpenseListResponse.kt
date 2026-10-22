package com.prgms.backend.domain.expense.dto

@JvmRecord
data class ExpenseListResponse(val currentMemberId: Long, val leader: Boolean, val editable: Boolean,
                               val expenses: List<ExpenseResponse>)
