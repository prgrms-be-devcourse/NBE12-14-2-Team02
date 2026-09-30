package com.prgms.backend.domain.expense.controller;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.dto.ExpenseResponse;
import com.prgms.backend.domain.expense.service.ExpenseRegistrationService;
import com.prgms.backend.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/expenses")
public class ExpenseRegistrationController {
    private final ExpenseRegistrationService registrations;

    @PostMapping(headers = "Idempotency-Key")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ExpenseResponse> create(@PathVariable long meetingId, Principal principal,
            @RequestHeader("Idempotency-Key") String requestKey, @Valid @RequestBody ExpenseCreateRequest request) {
        return ApiResponse.success(201, registrations.create(meetingId, principal, request, requestKey));
    }
}
