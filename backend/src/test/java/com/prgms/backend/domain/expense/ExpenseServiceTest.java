package com.prgms.backend.domain.expense;

import com.prgms.backend.domain.expense.dto.ExpenseCreateRequest;
import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.expense.service.ExpenseService;
import com.prgms.backend.domain.expense.service.ExpenseSplitCalculator;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.integration.MeetingAccessPort;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.global.exception.BusinessException;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExpenseServiceTest {
    private final ExpenseRepository expenses = mock(ExpenseRepository.class);
    private final SettlementRepository settlements = mock(SettlementRepository.class);
    private final MeetingMemberRepository members = mock(MeetingMemberRepository.class);
    private final MeetingAccessPort access = mock(MeetingAccessPort.class);
    private final Principal principal = () -> "user";
    private ValidatorFactory validation;
    private ExpenseService service;

    @BeforeEach void setup() {
        validation = Validation.buildDefaultValidatorFactory();
        service = new ExpenseService(expenses, new ExpenseSplitCalculator(), access, settlements, members, validation.getValidator());
        var context = new MeetingAccessPort.Context(1, true, true, Set.of(1L, 2L, 3L));
        when(access.requireMember(10, principal)).thenReturn(context);
        when(access.requireMemberForRead(10, principal)).thenReturn(context);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.empty());
        List<MeetingMember> joinedMembers = List.of(member(1), member(2));
        when(members.findAllByMeetingIdAndStatus(10L, MeetingMemberStatus.JOINED)).thenReturn(joinedMembers);
        when(expenses.save(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach void closeValidator() { validation.close(); }

    @Test @DisplayName("인증된 모임원이 등록하면 결제자와 분담액을 저장")
    void create() {
        var response = service.create(10, principal, request(100, 1, 100));
        assertEquals(1L, response.payerMemberId());
        assertEquals(100L, response.amount());
        assertEquals(100L, response.participants().getFirst().amount());
        verify(expenses).save(any(Expense.class));
    }

    @Test @DisplayName("본인 지출 수정은 ID·결제자·등록 시각·영수증을 유지")
    void updatePreservesIdentity() {
        Expense expense = existing(10, 1);
        LocalDateTime created = LocalDateTime.of(2026, 9, 1, 12, 0);
        ReflectionTestUtils.setField(expense, "createdAt", created);
        expense.attachReceipt("receipt/original");
        var response = service.update(10, 7, principal, request(200, 2, 200));
        assertEquals(7L, response.id());
        assertEquals(1L, response.payerMemberId());
        assertEquals(created, response.createdAt());
        assertEquals(Map.of(2L, 200L), expense.getParticipantAmounts());
        assertEquals(Set.of(2L), expense.getParticipantIds());
        assertEquals("receipt/original", expense.getReceiptKey());
        verify(expenses, never()).save(any()); // JPA 변경 감지 사용
    }

    @Test @DisplayName("다른 사람 지출은 모임장도 수정·삭제 불가")
    void otherOwner() {
        existing(10, 2);
        assertCode(403, () -> service.update(10, 7, principal, request(100, 1, 100)));
        assertCode(403, () -> service.delete(10, 7, principal));
        verify(expenses, never()).delete(any());
    }

    @Test @DisplayName("타 모임 지출은 조회·수정·삭제 불가")
    void foreignExpense() {
        existing(99, 1);
        assertCode(404, () -> service.get(10, 7, principal));
        assertCode(404, () -> service.update(10, 7, principal, request(100, 1, 100)));
        assertCode(404, () -> service.delete(10, 7, principal));
    }

    @Test @DisplayName("정산 확정 후 등록·수정·삭제를 모두 차단")
    void closedSettlement() {
        Settlement settlement = new Settlement(10);
        settlement.close(1);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.of(settlement));
        assertAllMutationsRejected();
    }

    @Test @DisplayName("모임 종료 후에도 모든 변경을 차단")
    void closedMeeting() {
        when(access.requireMember(10, principal)).thenReturn(new MeetingAccessPort.Context(1, true, false, Set.of(1L)));
        assertAllMutationsRejected();
    }

    @Test @DisplayName("분담 합계가 틀리거나 입력 검증에 실패하면 저장하지 않는다")
    void invalidInput() {
        assertCode(400, () -> service.create(10, principal, request(100, 1, 99)));
        assertCode(400, () -> service.create(10, principal, request(0, 1, 0)));
        verify(expenses, never()).save(any());
    }

    @Test @DisplayName("기존 탈퇴 부담자는 유지 가능하지만 새 탈퇴 부담자 추가는 불가")
    void previousParticipants() {
        Expense expense = existing(10, 1);
        expense.update("기존", 100, null, Map.of(3L, 100L), "EXACT", null, null);
        service.update(10, 7, principal, request(200, 3, 200));
        assertEquals(Map.of(3L, 200L), expense.getParticipantAmounts());
        assertCode(400, () -> service.create(10, principal, request(200, 3, 200)));
        assertCode(400, () -> service.update(10, 7, principal, request(200, 9, 200)));
    }

    @Test @DisplayName("본인 지출 삭제는 해당 지출만 삭제 요청")
    void delete() {
        Expense expense = existing(10, 1);
        service.delete(10, 7, principal);
        verify(expenses).delete(expense);
    }

    @Test @DisplayName("확정된 목록도 읽을 수 있지만 편집 가능 여부는 false")
    void readClosedList() {
        Settlement settlement = new Settlement(10); settlement.close(1);
        when(settlements.findByMeetingId(10)).thenReturn(Optional.of(settlement));
        Expense expense = existing(10, 1);
        when(expenses.findByMeetingIdOrderByIdAsc(10)).thenReturn(List.of(expense));
        var result = service.list(10, principal);
        assertFalse(result.editable()); assertEquals(1, result.expenses().size());
        verify(access, never()).requireMember(anyLong(), any());
    }

    private void assertAllMutationsRejected() {
        assertCode(409, () -> service.create(10, principal, request(100, 1, 100)));
        assertCode(409, () -> service.update(10, 7, principal, request(100, 1, 100)));
        assertCode(409, () -> service.delete(10, 7, principal));
        verify(expenses, never()).save(any()); verify(expenses, never()).delete(any());
    }
    private Expense existing(long meetingId, long payerId) {
        Expense expense = Expense.register(meetingId, payerId, "기존", 100, null, Map.of(1L, 100L), "EXACT", null, null);
        ReflectionTestUtils.setField(expense, "id", 7L);
        when(expenses.findById(7L)).thenReturn(Optional.of(expense));
        return expense;
    }
    private MeetingMember member(long id) {
        MeetingMember member = mock(MeetingMember.class); when(member.getId()).thenReturn(id); return member;
    }
    private ExpenseCreateRequest request(long total, long memberId, long share) {
        return new ExpenseCreateRequest("식사", BigDecimal.valueOf(total), null, ExpenseCreateRequest.SplitMode.EXACT,
                null, null, List.of(new ExpenseCreateRequest.Participant(memberId, BigDecimal.valueOf(share))), false);
    }
    private void assertCode(int code, org.junit.jupiter.api.function.Executable action) {
        assertEquals(code, assertThrows(BusinessException.class, action).getCode());
    }
}
