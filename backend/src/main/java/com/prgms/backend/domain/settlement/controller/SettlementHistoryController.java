package com.prgms.backend.domain.settlement.controller;

import com.prgms.backend.domain.settlement.service.SettlementHistoryService;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/settlements")
public class SettlementHistoryController {
    private final SettlementHistoryService history;

    @GetMapping("/mine")
    public ApiResponse<List<SettlementHistoryService.Entry>> mine(Principal principal) {
        return ApiResponse.success(200, history.getMine(principal));
    }
}
