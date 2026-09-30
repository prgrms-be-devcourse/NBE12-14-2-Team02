package com.prgms.backend.domain.expense;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest.Participant;
import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest.SplitMode;
import com.prgms.backend.domain.expense.exception.ExpenseRequestException;
import com.prgms.backend.domain.expense.service.ExpenseSplitCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
//정산 금액 테스트
class ExpenseSplitCalculatorTest {
    private final ExpenseSplitCalculator calculator = new ExpenseSplitCalculator();
    private final List<Participant> participants = List.of(new Participant(1L, null), new Participant(2L, null), new Participant(3L, null));

    @Test @DisplayName("3만원을 3명에게 균등 분담")
    void equal() {
        assertEquals(Map.of(1L, 10_000L, 2L, 10_000L, 3L, 10_000L), calculator.calculate(equal(30_000, 1, null, false)));
    }

    @ParameterizedTest @ValueSource(ints = {1, 10, 100, 1000})
    @DisplayName("분담 단위와 관계없이 차액 담당자에게 남은 금액을 배분")
    void units(int unit) {
        var result = calculator.calculateResult(equal(10_000, unit, 3L, false));
        long each = 10_000 / 3 / unit * unit;
        assertEquals(Map.of(1L, each, 2L, each, 3L, 10_000 - each * 2), result.shares());
        assertEquals(10_000L, result.shares().values().stream().mapToLong(Long::longValue).sum());
        assertEquals(3L, result.remainderMemberId());
    }

    @Test @DisplayName("랜덤 결과의 사람은 고정하지 않고 유효한 담당자와 금액 보존을 확인")
    void randomRemainder() {
        for (int i = 0; i < 50; i++) {
            var result = calculator.calculateResult(equal(10_000, 1, null, true));
            assertTrue(result.shares().containsKey(result.remainderMemberId()));
            assertEquals(3334L, result.shares().get(result.remainderMemberId()));
            assertEquals(10_000L, result.shares().values().stream().mapToLong(Long::longValue).sum());
        }
    }

    @Test @DisplayName("직접 지정 금액과 0원 부담자를 그대로 유지")
    void exact() {
        var request = exact(List.of(new Participant(1L, BigDecimal.ZERO), new Participant(2L, BigDecimal.valueOf(10_000))));
        assertEquals(Map.of(1L, 0L, 2L, 10_000L), calculator.calculate(request));
    }

    @Test @DisplayName("금액 합계 불일치·누락·음수 입력을 거절")
    void invalidExact() {
        for (var amount : List.of(BigDecimal.valueOf(9999), BigDecimal.valueOf(-1))) {
            assertThrows(ExpenseRequestException.class, () -> calculator.calculate(exact(List.of(new Participant(1L, amount)))));
        }
        assertThrows(ExpenseRequestException.class, () -> calculator.calculate(exact(List.of(new Participant(1L, null)))));
    }

    @Test @DisplayName("차액 담당자 누락·외부인·옵션 충돌·잘못된 단위를 거절")
    void invalidOptions() {
        for (var request : List.of(equal(10_000, 1, null, false), equal(10_000, 1, 9L, false),
                equal(10_000, 1, 1L, true), equal(10_000, 7, 1L, false))) {
            assertEquals(400, assertThrows(ExpenseRequestException.class, () -> calculator.calculate(request)).getCode());
        }
    }

    @Test @DisplayName("참여자 중복과 빈 목록을 거절")
    void invalidParticipants() {
        for (List<Participant> list : List.of(List.<Participant>of(), List.of(new Participant(1L, null), new Participant(1L, null)))) {
            var request = new ExpenseCreateRequest("식사", BigDecimal.TEN, null, SplitMode.EQUAL, 1, null, list, false);
            assertThrows(ExpenseRequestException.class, () -> calculator.calculate(request));
        }
    }

    private ExpenseCreateRequest equal(long amount, int unit, Long remainder, boolean random) {
        return new ExpenseCreateRequest("식사", BigDecimal.valueOf(amount), null, SplitMode.EQUAL, unit, remainder, participants, random);
    }

    private ExpenseCreateRequest exact(List<Participant> participants) {
        return new ExpenseCreateRequest("식사", BigDecimal.valueOf(10_000), null, SplitMode.EXACT, null, null, participants, false);
    }
}
