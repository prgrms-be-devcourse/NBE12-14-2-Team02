package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.exception.ExpenseRequestException;

import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class ExpenseSplitCalculator {
    public record Result(Map<Long, Long> shares, Long remainderMemberId) {
        public Result { shares = Map.copyOf(shares); }
    }

    public Map<Long, Long> calculate(ExpenseCreateRequest request) {
        return calculateResult(request).shares();
    }

    public Result calculateResult(ExpenseCreateRequest request) {
        Long selectedMemberId = null;
        long total = request.amount().longValueExact();
        if (total <= 0) throw invalid("지출 금액은 1원 이상이어야 합니다.");
        Map<Long, Long> shares = new TreeMap<>();
        for (var participant : request.participants()) {
            if (shares.putIfAbsent(participant.memberId(), 0L) != null) throw invalid("참여자가 중복되었습니다.");
        }
        if (shares.isEmpty()) throw invalid("참여자를 한 명 이상 선택해주세요.");
        if (request.splitMode() == ExpenseCreateRequest.SplitMode.EXACT) {
            if (request.roundingUnit() != null || request.remainderMemberId() != null || request.randomRemainder()) {
                throw invalid("금액 지정 분담에는 분담 단위와 차액 담당자를 지정할 수 없습니다.");
            }
            long sum = 0;
            for (var participant : request.participants()) {
                if (participant.amount() == null) throw invalid("각 참여자의 부담 금액을 입력해주세요.");
                long amount = participant.amount().longValueExact();
                if (amount < 0 || amount > total) throw invalid("개인 부담액은 0원 이상, 지출 금액 이하여야 합니다.");
                sum = Math.addExact(sum, amount);
                shares.put(participant.memberId(), amount);
            }
            if (sum != total) throw invalid("개인 부담액의 합계가 지출 금액과 같아야 합니다.");
        } else {
            if (request.randomRemainder() && request.remainderMemberId() != null) {
                throw invalid("차액 담당자 직접 지정과 랜덤 선택 중 하나만 사용해주세요.");
            }
            int unit = request.roundingUnit() == null ? 1 : request.roundingUnit();
            if (!Set.of(1, 10, 100, 1000).contains(unit)) throw invalid("분담 단위는 1, 10, 100, 1000원 중 선택해주세요.");
            if (request.participants().stream().anyMatch(p -> p.amount() != null)) {
                throw invalid("균등 분담에는 개인 금액을 입력하지 마세요.");
            }
            if (request.remainderMemberId() != null && !shares.containsKey(request.remainderMemberId())) {
                throw invalid("차액 담당자는 해당 지출 참여자여야 합니다.");
            }
            long each = total / shares.size() / unit * unit;
            long remainder = total - each * shares.size();
            shares.replaceAll((id, ignored) -> each);
            if (remainder > 0) {
                if (request.randomRemainder()) {
                    var candidates = new ArrayList<>(shares.keySet());
                    selectedMemberId = candidates.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(candidates.size()));
                } else {
                    selectedMemberId = request.remainderMemberId();
                    if (selectedMemberId == null) throw invalid("남은 차액을 부담할 참여자를 선택해주세요.");
                }
                shares.put(selectedMemberId, each + remainder);
            }
        }
        return new Result(shares, selectedMemberId);
    }

    private ExpenseRequestException invalid(String message) { return new ExpenseRequestException(400, message); }
}
