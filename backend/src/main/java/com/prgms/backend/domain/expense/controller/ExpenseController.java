//package com.prgms.backend.domain.expense.controller;
//
//import com.prgms.backend.domain.expense.dto.*;
//import com.prgms.backend.domain.expense.service.ExpenseService;
//import com.prgms.backend.global.ApiResponse;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.*;
//import java.security.Principal;
//
//@RestController
//@RequestMapping("/api/meetings/{meetingId}/expenses")
//@RequiredArgsConstructor
//public class ExpenseController {
//    private final ExpenseService service;
//
//    @PostMapping
//    @ResponseStatus(HttpStatus.CREATED)
//    public ApiResponse<ExpenseResponse> create(@PathVariable long meetingId, Principal principal,
//                                               @Valid @RequestBody ExpenseCreateRequest request) {
//        return ApiResponse.success(201, service.create(meetingId, principal, request));
//    }
//}
