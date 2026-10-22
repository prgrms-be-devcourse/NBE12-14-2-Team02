package com.prgms.backend.domain.settlement.controller

import com.prgms.backend.domain.settlement.dto.*
import com.prgms.backend.domain.settlement.service.SettlementService
import com.prgms.backend.domain.settlement.service.SettlementAccountService
import com.prgms.backend.global.ApiResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/meetings/{meetingId}/settlement")
class SettlementController(private val settlementService: SettlementService, private val accountService: SettlementAccountService) {
    @GetMapping("/preview")
    fun preview(@PathVariable meetingId: Long, principal: Principal?): ApiResponse<SettlementPreviewResponse> =
        ApiResponse.success(200, settlementService.preview(meetingId, principal))

    @GetMapping("/account")
    fun account(@PathVariable meetingId: Long, principal: Principal?): ApiResponse<AccountResponse> =
        ApiResponse.success(200, accountService.get(meetingId, principal))

    @PutMapping("/account")
    fun saveAccount(@PathVariable meetingId: Long, principal: Principal?,
                    @Valid @RequestBody request: AccountRequest): ApiResponse<AccountResponse> =
        ApiResponse.success(200, accountService.save(meetingId, principal, request))

    @PostMapping("/confirm")
    fun confirm(@PathVariable meetingId: Long, principal: Principal?): ApiResponse<SettlementResponse> =
        ApiResponse.success(200, settlementService.confirm(meetingId, principal))

    @GetMapping
    fun getResult(@PathVariable meetingId: Long, principal: Principal?): ApiResponse<SettlementResponse> =
        ApiResponse.success(200, settlementService.getResult(meetingId, principal))
}
