package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.dto.*;
import com.prgms.backend.domain.expense.entity.Expense;

import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository repository;
    private final ExpenseSplitCalculator calculator;
    private final ObjectProvider<MeetingAccessPort> meetingAccess;
    private final Validator validator;

    @Transactional
    public ExpenseResponse create(long meetingId, Principal principal, ExpenseCreateRequest request) {
        if (principal == null) throw new IllegalStateException("로그인이 필요합니다.");
        if (meetingId <= 0) throw new IllegalArgumentException("모임 ID를 확인해주세요.");
        var adapter = meetingAccess.getIfAvailable();
        if (adapter == null) throw new IllegalStateException("회원·모임 연동이 아직 준비되지 않았습니다.");
        var context = adapter.requireMember(meetingId, principal);
        if (context == null || !context.memberIds().contains(context.memberId())) {
            throw new IllegalStateException("해당 모임의 참여자만 등록할 수 있습니다.");
        }
        if (!context.meetingOpen()) throw new IllegalStateException("종료된 모임에는 등록할 수 없습니다.");
        if (request == null || !validator.validate(request).isEmpty()) throw new IllegalArgumentException("지출 입력값을 확인해주세요.");
        var result = calculator.calculateResult(request);
        var shares = result.shares();
        if (!context.memberIds().containsAll(shares.keySet())) throw new IllegalArgumentException("다른 모임의 참여자가 포함되어 있습니다.");
        var expense = Expense.register(meetingId, context.memberId(), request.title(), request.amount().longValueExact(),
                request.memo(), shares, request.splitMode().name(),
                request.splitMode() == ExpenseCreateRequest.SplitMode.EQUAL
                        ? (request.roundingUnit() == null ? 1 : request.roundingUnit()) : null,
                result.remainderMemberId());
        return ExpenseResponse.from(repository.save(expense));
    }
}
