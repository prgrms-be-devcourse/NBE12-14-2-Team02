package com.prgms.backend.domain.settlement;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.settlement.service.SettlementCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SettlementCalculatorTest {
    private final SettlementCalculator calculator = new SettlementCalculator();
    private final Set<Long> members = Set.of(1L, 2L, 3L);

    @Test @DisplayName("한 명이 전액 결제: 지출 3만원, 실제 송금은 2만원")
    void onePayer() {
        var expenses = List.of(expense(1, 30_000, Map.of(1L, 10_000L, 2L, 10_000L, 3L, 10_000L)));
        var result = calculator.calculate(10, expenses, members);
        assertEquals(List.of(new SettlementCalculator.Transfer(2, 1, 10_000),
                new SettlementCalculator.Transfer(3, 1, 10_000)), result.transfers());
        assertConservation(expenses, result);
    }

    @Test @DisplayName("여러 결제자의 지정 부담액을 다시 균등 분배하지 않는다")
    void multiplePayers() {
        var expenses = List.of(expense(1, 30_000, Map.of(1L, 20_000L, 2L, 10_000L)),
                expense(2, 12_000, Map.of(1L, 4_000L, 2L, 4_000L, 3L, 4_000L)));
        var result = calculator.calculate(10, expenses, members);
        assertEquals(List.of(new SettlementCalculator.Balance(1, 30_000, 24_000),
                new SettlementCalculator.Balance(2, 12_000, 14_000),
                new SettlementCalculator.Balance(3, 0, 4_000)), result.balances());
        assertEquals(List.of(new SettlementCalculator.Transfer(2, 1, 2_000),
                new SettlementCalculator.Transfer(3, 1, 4_000)), result.transfers());
        assertConservation(expenses, result);
    }

    @Test @DisplayName("부담자가 아닌 결제자는 대신 낸 금액을 전부 돌려받는다")
    void payerOutsideParticipants() {
        var expenses = List.of(expense(1, 101, Map.of(2L, 50L, 3L, 51L)));
        var result = calculator.calculate(10, expenses, members);
        assertEquals(List.of(new SettlementCalculator.Transfer(2, 1, 50),
                new SettlementCalculator.Transfer(3, 1, 51)), result.transfers());
        assertConservation(expenses, result);
    }

    @Test @DisplayName("각자 부담액만큼 결제하면 송금이 없다")
    void balanced() {
        var expenses = List.of(expense(1, 100, Map.of(1L, 50L, 2L, 50L)),
                expense(2, 100, Map.of(1L, 50L, 2L, 50L)));
        var result = calculator.calculate(10, expenses, members);
        assertTrue(result.transfers().isEmpty());
        assertConservation(expenses, result);
    }

    @Test @DisplayName("지출이 없으면 모든 금액이 0원이다")
    void empty() {
        var result = calculator.calculate(10, List.of(), members);
        assertEquals(3, result.balances().size());
        assertTrue(result.transfers().isEmpty());
        assertConservation(List.of(), result);
    }

    @Test @DisplayName("기존 지출의 1원 차액을 ID 순서로 배분한다")
    void legacyRemainder() {
        var result = calculator.calculate(10, List.of(new Expense(10, 1, "기존", 10, null, members)), members);
        assertEquals(List.of(new SettlementCalculator.Balance(1, 10, 4),
                new SettlementCalculator.Balance(2, 0, 3), new SettlementCalculator.Balance(3, 0, 3)), result.balances());
    }

    @Test @DisplayName("같은 객체 또는 같은 DB ID의 중복 지출을 거절한다")
    void duplicates() {
        Expense first = expense(1, 100, Map.of(2L, 100L));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(10, List.of(first, first), members));
        Expense second = expense(1, 100, Map.of(2L, 100L));
        ReflectionTestUtils.setField(first, "id", 7L);
        ReflectionTestUtils.setField(second, "id", 7L);
        assertThrows(IllegalArgumentException.class, () -> calculator.calculate(10, List.of(first, second), members));
    }

    @Test @DisplayName("다른 모임·잘못된 참여자·음수 부담액·합계 불일치를 거절한다")
    void invalid() {
        var invalid = List.of(
                Expense.register(99, 1, "타 모임", 100, null, Map.of(2L, 100L), "EXACT", null, null),
                expense(1, 100, Map.of(9L, 100L)), expense(9, 100, Map.of(1L, 100L)),
                expense(1, 100, Map.of(1L, -1L, 2L, 101L)), expense(1, 100, Map.of(1L, 99L)));
        for (Expense expense : invalid) {
            assertThrows(IllegalArgumentException.class, () -> calculator.calculate(10, List.of(expense), members));
        }
    }

    @Test @DisplayName("고정 시드의 200개 사례에서 전체·개인별 금액이 보존된다")
    void generatedCases() {
        Random random = new Random(20260923);
        for (int scenario = 0; scenario < 200; scenario++) {
            List<Expense> expenses = new ArrayList<>();
            for (int index = 0; index < 10; index++) {
                Map<Long, Long> shares = new HashMap<>();
                for (long id : members) shares.put(id, 1L + random.nextInt(100_000));
                long total = shares.values().stream().mapToLong(Long::longValue).sum();
                expenses.add(expense(1 + random.nextInt(3), total, shares));
            }
            assertConservation(expenses, calculator.calculate(10, expenses, members));
        }
    }

    private Expense expense(long payer, long amount, Map<Long, Long> shares) {
        return Expense.register(10, payer, "식사", amount, null, shares, "EXACT", null, null);
    }

    // 합계만 맞고 대상이 틀리는 오류도 잡도록 개인별 송금 후 부담 검사
    private void assertConservation(List<Expense> expenses, SettlementCalculator.Result result) {
        long total = expenses.stream().mapToLong(Expense::getAmount).sum();
        assertEquals(total, result.balances().stream().mapToLong(SettlementCalculator.Balance::paidAmount).sum());
        assertEquals(total, result.balances().stream().mapToLong(SettlementCalculator.Balance::shareAmount).sum());
        long sent = result.transfers().stream().mapToLong(SettlementCalculator.Transfer::amount).sum();
        assertEquals(sent, result.balances().stream().mapToLong(b -> Math.max(0, b.netAmount())).sum());
        assertEquals(sent, result.balances().stream().mapToLong(b -> Math.max(0, -b.netAmount())).sum());
        for (var transfer : result.transfers()) {
            assertTrue(transfer.amount() > 0);
            assertNotEquals(transfer.senderId(), transfer.recipientId());
            assertTrue(members.contains(transfer.senderId()) && members.contains(transfer.recipientId()));
        }
        for (var balance : result.balances()) {
            long outgoing = result.transfers().stream().filter(t -> t.senderId() == balance.memberId())
                    .mapToLong(SettlementCalculator.Transfer::amount).sum();
            long incoming = result.transfers().stream().filter(t -> t.recipientId() == balance.memberId())
                    .mapToLong(SettlementCalculator.Transfer::amount).sum();
            assertEquals(balance.shareAmount(), balance.paidAmount() + outgoing - incoming,
                    "모임원 " + balance.memberId() + "의 최종 부담액");
        }
    }
}
