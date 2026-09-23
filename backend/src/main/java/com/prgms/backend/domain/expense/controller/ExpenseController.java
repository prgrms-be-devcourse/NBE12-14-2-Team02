package com.prgms.backend.domain.expense.controller;

import com.prgms.backend.domain.expense.dto.*;
import com.prgms.backend.domain.expense.service.ExpenseService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/meetings/{meetingId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {
    private final ExpenseService service;

    @GetMapping
    public ApiResponse<ExpenseListResponse> list(@PathVariable long meetingId, Principal principal) {
        return ApiResponse.success(200, service.list(meetingId, principal));
    }

    @GetMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> get(@PathVariable long meetingId, @PathVariable long expenseId,
                                           Principal principal) {
        return ApiResponse.success(200, service.get(meetingId, expenseId, principal));
    }

    @PutMapping("/{expenseId}")
    public ApiResponse<ExpenseResponse> update(@PathVariable long meetingId, @PathVariable long expenseId,
            Principal principal, @Valid @RequestBody ExpenseCreateRequest request) {
        return ApiResponse.success(200, service.update(meetingId, expenseId, principal, request));
    }

    @DeleteMapping("/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long meetingId, @PathVariable long expenseId, Principal principal) {
        service.delete(meetingId, expenseId, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(@PathVariable long meetingId, Principal principal,
                                               @Valid @RequestBody ExpenseCreateRequest request) {
        return ApiResponse.success(201, service.create(meetingId, principal, request));
    }
}
