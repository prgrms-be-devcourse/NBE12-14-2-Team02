package com.prgms.backend.domain.settlement.controller;

import com.prgms.backend.domain.settlement.dto.SettlementResponse;
import com.prgms.backend.domain.settlement.service.SettlementService;
import com.prgms.backend.global.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings/{meetingId}/settlement")
public class SettlementController {
    private final SettlementService settlementService;

    // 모임장의 정산 확정 버튼에서 호출합니다. 사용자 ID는 요청으로 받지 않습니다.
    @PostMapping("/confirm")
    public ApiResponse<SettlementResponse> confirm(@PathVariable long meetingId, Principal principal) {
        return ApiResponse.success(200, settlementService.confirm(meetingId, principal));
    }

    @GetMapping
    public ApiResponse<SettlementResponse> getResult(@PathVariable long meetingId, Principal principal) {
        return ApiResponse.success(200, settlementService.getResult(meetingId, principal));
    }
}
