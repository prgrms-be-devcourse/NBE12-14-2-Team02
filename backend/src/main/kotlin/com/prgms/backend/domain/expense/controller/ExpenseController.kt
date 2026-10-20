package com.prgms.backend.domain.expense.controller

import com.prgms.backend.domain.expense.dto.*
import com.prgms.backend.domain.expense.service.ExpenseService
import com.prgms.backend.global.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/meetings/{meetingId}/expenses")
class ExpenseController(private val service: ExpenseService) {
    @GetMapping
    fun list(@PathVariable meetingId: Long, principal: Principal?): ApiResponse<ExpenseListResponse> =
        ApiResponse.success(200, service.list(meetingId, principal))

    @GetMapping("/{expenseId}")
    fun get(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?): ApiResponse<ExpenseResponse> =
        ApiResponse.success(200, service.get(meetingId, expenseId, principal))

    @PutMapping("/{expenseId}")
    fun update(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?,
               @Valid @RequestBody request: ExpenseCreateRequest): ApiResponse<ExpenseResponse> =
        ApiResponse.success(200, service.update(meetingId, expenseId, principal, request))

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@PathVariable meetingId: Long, @PathVariable expenseId: Long, principal: Principal?) {
        service.delete(meetingId, expenseId, principal)
    }

    @PostMapping(headers = ["!Idempotency-Key"])
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@PathVariable meetingId: Long, principal: Principal?,
               @Valid @RequestBody request: ExpenseCreateRequest): ApiResponse<ExpenseResponse> =
        ApiResponse.success(201, service.create(meetingId, principal, request))
}
