package com.prgms.backend.domain.settlement.controller

import com.prgms.backend.domain.settlement.service.SettlementHistoryService
import com.prgms.backend.global.ApiResponse
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/api/settlements")
class SettlementHistoryController(private val history: SettlementHistoryService) {
    @GetMapping("/mine")
    fun mine(principal: Principal?): ApiResponse<List<SettlementHistoryService.Entry>> =
        ApiResponse.success(200, history.getMine(principal))
}
