package com.prgms.backend.domain.expense.controller

import com.prgms.backend.domain.expense.dto.*
import com.prgms.backend.domain.expense.service.ExpenseRegistrationService
import com.prgms.backend.global.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/meetings/{meetingId}/expenses")
class ExpenseRegistrationController(private val registrations: ExpenseRegistrationService) {
    @PostMapping(headers = ["Idempotency-Key"])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable meetingId: Long, principal: Principal?, @RequestHeader("Idempotency-Key") requestKey: String,
               @Valid @RequestBody request: ExpenseCreateRequest): ApiResponse<ExpenseResponse> =
        ApiResponse.success(201, registrations.create(meetingId, principal, request, requestKey))
}
