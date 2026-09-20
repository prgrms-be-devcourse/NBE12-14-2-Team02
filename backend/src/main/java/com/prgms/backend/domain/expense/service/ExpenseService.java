package com.prgms.backend.domain.expense.service;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.dto.ExpenseResponse;
import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ExpenseService {
    private final ExpenseRepository expenseRepository;
    private final ExpenseSplitCalculator splitCalculator;
    private final MeetingAccessPort meetingAccess;
    private final SettlementRepository settlementRepository;
    private final MeetingMemberRepository memberRepository;
    private final Validator validator;

    @Transactional
    public ExpenseResponse create(long meetingId, Principal principal, ExpenseCreateRequest request) {
        if (meetingId <= 0) {
            throw new IllegalArgumentException("모임 ID를 확인해주세요.");
        }
        // 정산 확정과 같은 모임 잠금을 사용합니다. 먼저 시작한 작업이 끝난 뒤 다음 작업을 처리합니다.
        MeetingAccessPort.Context context = meetingAccess.requireMember(meetingId, principal);
        checkRegistrationAllowed(meetingId, context);
        validateRequest(request);

        ExpenseSplitCalculator.Result splitResult = splitCalculator.calculateResult(request);
        Map<Long, Long> shares = splitResult.shares();
        checkParticipants(meetingId, shares.keySet());

        Expense expense = Expense.register(
                meetingId,
                context.memberId(),
                request.title(),
                request.amount().longValueExact(),
                request.memo(),
                shares,
                request.splitMode().name(),
                getRoundingUnit(request),
                splitResult.remainderMemberId()
        );
        return ExpenseResponse.from(expenseRepository.save(expense));
    }

    private void checkRegistrationAllowed(long meetingId, MeetingAccessPort.Context context) {
        if (!context.meetingOpen()) {
            throw new IllegalStateException("종료된 모임에는 등록할 수 없습니다.");
        }

        //정산 데이터가 없거나 마감 안했으면 등록 가능(open상태), 확정 누르면 closed로 등록 안됨
        settlementRepository.findByMeetingId(meetingId).ifPresent(settlement -> settlement.checkOpen());
    }

    //
    private void validateRequest(ExpenseCreateRequest request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new IllegalArgumentException("지출 입력값을 확인해주세요.");
        }
    }

    // 모임에 참여했다가 탈퇴한 회원은 정산에 참여 x,
    private void checkParticipants(long meetingId, Set<Long> participantIds) {
        Set<Long> joinedMemberIds = new HashSet<>();
        for (var member : memberRepository.findAllByMeetingIdAndStatus(meetingId, MeetingMemberStatus.JOINED)) {
            joinedMemberIds.add(member.getId());
        }

        if (!joinedMemberIds.containsAll(participantIds)) {
            throw new IllegalArgumentException("현재 모임에 참여 중인 모임원만 지정할 수 있습니다.");
        }
    }

    private Integer getRoundingUnit(ExpenseCreateRequest request) {
        if (request.splitMode() != ExpenseCreateRequest.SplitMode.EQUAL) {
            return null;
        }
        return request.roundingUnit() == null ? 1 : request.roundingUnit();
    }
}
