package com.prgms.backend.domain.settlement;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.settlement.entity.*;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.settlement.service.*;
import com.prgms.backend.global.exception.BusinessException;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import java.security.Principal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SettlementServiceTest {
    private final MeetingAccessPort access = mock(MeetingAccessPort.class);
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final SettlementRepository settlements = mock(SettlementRepository.class);
    private final SettlementService service = new SettlementService(access, expenses, settlements, new SettlementCalculator());
    private final Principal principal = () -> "host";

    @BeforeEach void setup() {
        when(access.requireMember(10, principal)).thenReturn(context(true, true));
        when(access.requireMemberForRead(10, principal)).thenReturn(context(false, true));
        when(settlements.findByMeetingId(10)).thenReturn(Optional.empty());
        when(settlements.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test @DisplayName("모임장이 확정하면 금액·송금 내역·확정자·시각을 함께 저장")
    void confirm() {
        when(expenses.findByMeetingIdOrderByIdAsc(10)).thenReturn(List.of(
                Expense.register(10, 1, "식사", 300, null, Map.of(1L, 100L, 2L, 200L), "EXACT", null, null)));
        var result = service.confirm(10, principal);
        assertEquals(SettlementStatus.CLOSED, result.status());
        assertEquals(1L, result.closedByMemberId()); assertNotNull(result.closedAt());
        assertEquals(List.of(new SettlementCalculator.Transfer(2, 1, 200)), result.transfers());
        ArgumentCaptor<Settlement> saved = ArgumentCaptor.forClass(Settlement.class);
        verify(settlements).save(saved.capture());
        assertEquals(2, saved.getValue().getBalances().size());
        assertEquals(200, saved.getValue().getTransfers().getFirst().getAmount());
    }

    @Test @DisplayName("일반 모임원은 확정할 수 없다")
    void memberCannotConfirm() {
        when(access.requireMember(10, principal)).thenReturn(context(false, true));
        assertEquals(403, assertThrows(BusinessException.class, () -> service.confirm(10, principal)).getCode());
        verifyNoInteractions(expenses); verify(settlements, never()).save(any());
    }

    @Test @DisplayName("종료된 모임과 이미 확정된 정산을 거절")
    void closed() {
        when(access.requireMember(10, principal)).thenReturn(context(true, false));
        assertEquals(409, assertThrows(BusinessException.class, () -> service.confirm(10, principal)).getCode());
        when(access.requireMember(10, principal)).thenReturn(context(true, true));
        Settlement settlement = new Settlement(10); settlement.close(1);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.of(settlement));
        assertEquals(409, assertThrows(BusinessException.class, () -> service.confirm(10, principal)).getCode());
        verify(settlements, never()).save(any());
    }

    @Test @DisplayName("잘못된 부담액으로 계산에 실패하면 OPEN 상태와 빈 결과를 유지")
    void failedCalculation() {
        Settlement settlement = new Settlement(10);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.of(settlement));
        when(expenses.findByMeetingIdOrderByIdAsc(10)).thenReturn(List.of(
                Expense.register(10, 1, "오류", 100, null, Map.of(1L, 99L), "EXACT", null, null)));
        assertThrows(IllegalArgumentException.class, () -> service.confirm(10, principal));
        assertEquals(SettlementStatus.OPEN, settlement.getStatus());
        assertTrue(settlement.getBalances().isEmpty()); assertNull(settlement.getClosedAt());
        verify(settlements, never()).save(any());
    }

    @Test @DisplayName("미확정 조회는 404, 확정 결과는 재계산 없이 조회")
    void read() {
        assertEquals(404, assertThrows(BusinessException.class, () -> service.getResult(10, principal)).getCode());
        Settlement settlement = new Settlement(10);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.of(settlement));
        assertEquals(404, assertThrows(BusinessException.class, () -> service.getResult(10, principal)).getCode());
        settlement.close(1);
        assertEquals(SettlementStatus.CLOSED, service.getResult(10, principal).status());
        verifyNoInteractions(expenses);
    }

    private MeetingAccessPort.Context context(boolean leader, boolean open) {
        return new MeetingAccessPort.Context(1, leader, open, Set.of(1L, 2L));
    }
}
