package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingMemberService;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostCannotLeaveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberHasUnsettledExpenseException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MeetingMemberServiceTest {

    private MeetingMemberRepository meetingMemberRepository;
    private MeetingRepository meetingRepository;
    private ExpenseRepository expenseRepository;
    private SettlementRepository settlementRepository;
    private ScheduleVoteRepository scheduleVoteRepository;
    private ContentVoteRepository contentVoteRepository;

    private MeetingMemberService meetingMemberService;

    private Meeting meeting;
    private MeetingMember hostMember;
    private MeetingMember member;

    @BeforeEach
    void setUp() {
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        meetingRepository = mock(MeetingRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        scheduleVoteRepository = mock(ScheduleVoteRepository.class);
        contentVoteRepository = mock(ContentVoteRepository.class);

        meetingMemberService = new MeetingMemberService(
            meetingMemberRepository,
            meetingRepository,
            expenseRepository,
            settlementRepository,
            scheduleVoteRepository,
            contentVoteRepository
        );

        User host = new User(
            "host@test.com",
            "모임장",
            "password"
        );
        ReflectionTestUtils.setField(host, "id", 1L);

        User user = new User(
            "member@test.com",
            "일반 모임원",
            "password"
        );
        ReflectionTestUtils.setField(user, "id", 2L);

        meeting = new Meeting(host, "테스트 모임", "모임 설명");
        ReflectionTestUtils.setField(meeting, "id", 10L);

        hostMember = new MeetingMember(meeting, host);
        ReflectionTestUtils.setField(hostMember, "id", 100L);

        member = new MeetingMember(meeting, user);
        ReflectionTestUtils.setField(member, "id", 200L);

        // 존재하는 진행 중 모임 + 참여 중인 모임원
        // 정산 없음 + 일반 모임원은 지출 관련자가 아님
        when(meetingRepository
            .findByIdAndDeletedAtIsNullForUpdate(10L))
            .thenReturn(Optional.of(meeting));

        when(meetingMemberRepository
            .findByMeetingIdAndUserId(10L, 1L))
            .thenReturn(Optional.of(hostMember));

        when(meetingMemberRepository
            .findByMeetingIdAndUserId(10L, 2L))
            .thenReturn(Optional.of(member));

        when(settlementRepository.findByMeetingId(10L))
            .thenReturn(Optional.empty());

        when(expenseRepository.existsMemberInExpense(10L, 200L))
            .thenReturn(false);
    }

    @Test
    @DisplayName("지출에 관련되지 않은 일반 모임원은 탈퇴할 수 있다")
    void leaveMeeting_success() {
        // given
        LocalDateTime originalJoinedAt = member.getJoinedAt();

        // when
        var response = meetingMemberService.leaveMeeting(10L, 2L);

        // then: 실제 엔티티의 상태
        assertEquals(MeetingMemberStatus.LEFT, member.getStatus());
        assertNotNull(member.getLeftAt());
        assertEquals(originalJoinedAt, member.getJoinedAt());

        // then: 반환된 응답
        assertEquals(200L, response.id());
        assertEquals(10L, response.meetingId());
        assertEquals(2L, response.userId());
        assertEquals(MeetingMemberStatus.LEFT, response.status());
        assertEquals(member.getLeftAt(), response.leftAt());

        // then: 진행 중 투표 정리 요청
        verify(scheduleVoteRepository).deleteOpenVotesByMeetingMember(
            eq(10L),
            eq(200L),
            eq(SchedulePollStatus.OPEN),
            any(LocalDateTime.class)
        );

        verify(contentVoteRepository).deleteOpenVotesByMeetingMember(
            eq(10L),
            eq(200L),
            eq(ContentPollStatus.OPEN),
            any(LocalDateTime.class)
        );
    }

    @Test
    @DisplayName("진행 중인 모임의 모임장은 탈퇴할 수 없다")
    void leaveMeeting_activeHost() {
        // given
        LocalDateTime originalJoinedAt = hostMember.getJoinedAt();

        // when & then
        assertThrows(
            MeetingHostCannotLeaveException.class,
            () -> meetingMemberService.leaveMeeting(10L, 1L)
        );

        assertEquals(MeetingMemberStatus.JOINED, hostMember.getStatus());
        assertEquals(originalJoinedAt, hostMember.getJoinedAt());
        assertNull(hostMember.getLeftAt());

        verifyNoInteractions(
            settlementRepository,
            expenseRepository,
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("완료된 모임의 모임장은 탈퇴할 수 있다")
    void leaveMeeting_completedHost() {
        // given
        meeting.complete();

        when(expenseRepository.existsMemberInExpense(10L, 100L))
            .thenReturn(false);

        // when
        var response = meetingMemberService.leaveMeeting(10L, 1L);

        // then
        assertEquals(MeetingMemberStatus.LEFT, hostMember.getStatus());
        assertNotNull(hostMember.getLeftAt());
        assertEquals(100L, response.id());

        // 모임 탈퇴가 모임 자체를 삭제하면 안 된다.
        assertTrue(meeting.isCompleted());
        assertNull(meeting.getDeletedAt());
    }

    @Test
    @DisplayName("최종 정산이 없고 지출에 관련된 모임원은 탈퇴할 수 없다")
    void leaveMeeting_unsettledExpense() {
        // given
        when(expenseRepository.existsMemberInExpense(10L, 200L))
            .thenReturn(true);

        LocalDateTime originalJoinedAt = member.getJoinedAt();

        // when & then
        assertThrows(
            MeetingMemberHasUnsettledExpenseException.class,
            () -> meetingMemberService.leaveMeeting(10L, 2L)
        );

        assertEquals(MeetingMemberStatus.JOINED, member.getStatus());
        assertEquals(originalJoinedAt, member.getJoinedAt());
        assertNull(member.getLeftAt());

        verifyNoInteractions(
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("OPEN 정산이 있고 지출에 관련된 모임원은 탈퇴할 수 없다")
    void leaveMeeting_openSettlementWithExpense() {
        // given: 생성 직후 Settlement는 OPEN이다.
        Settlement settlement = new Settlement(10L);

        when(settlementRepository.findByMeetingId(10L))
            .thenReturn(Optional.of(settlement));

        when(expenseRepository.existsMemberInExpense(10L, 200L))
            .thenReturn(true);

        // when & then
        assertThrows(
            MeetingMemberHasUnsettledExpenseException.class,
            () -> meetingMemberService.leaveMeeting(10L, 2L)
        );

        assertEquals(MeetingMemberStatus.JOINED, member.getStatus());
        assertNull(member.getLeftAt());

        verifyNoInteractions(
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("OPEN 정산이 있어도 지출에 관련되지 않은 모임원은 탈퇴할 수 있다")
    void leaveMeeting_openSettlementWithoutExpense() {
        // given
        Settlement settlement = new Settlement(10L);

        when(settlementRepository.findByMeetingId(10L))
            .thenReturn(Optional.of(settlement));

        // 지출 관련 여부는 setUp에서 false로 설정되어 있다.

        // when
        var response = meetingMemberService.leaveMeeting(10L, 2L);

        // then
        assertEquals(MeetingMemberStatus.LEFT, response.status());
        assertNotNull(member.getLeftAt());
    }

    @Test
    @DisplayName("최종 정산이 CLOSED이면 지출 관련자도 탈퇴할 수 있다")
    void leaveMeeting_closedSettlement() {
        // given
        Settlement settlement = new Settlement(10L);
        settlement.close(100L);

        when(settlementRepository.findByMeetingId(10L))
            .thenReturn(Optional.of(settlement));

        when(expenseRepository.existsMemberInExpense(10L, 200L))
            .thenReturn(true);

        // when
        var response = meetingMemberService.leaveMeeting(10L, 2L);

        // then
        assertEquals(MeetingMemberStatus.LEFT, response.status());
        assertNotNull(member.getLeftAt());

        // CLOSED라면 지출 관련 여부로 탈퇴를 제한하지 않는다.
        verify(expenseRepository, never())
            .existsMemberInExpense(anyLong(), anyLong());
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 모임에서는 탈퇴할 수 없다")
    void leaveMeeting_meetingNotFound() {
        // given
        when(meetingRepository
            .findByIdAndDeletedAtIsNullForUpdate(10L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingMemberService.leaveMeeting(10L, 2L)
        );

        verifyNoInteractions(
            meetingMemberRepository,
            settlementRepository,
            expenseRepository,
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("가입한 적 없는 모임에서는 탈퇴할 수 없다")
    void leaveMeeting_memberNotFound() {
        // given
        when(meetingMemberRepository
            .findByMeetingIdAndUserId(10L, 2L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingMemberNotFoundException.class,
            () -> meetingMemberService.leaveMeeting(10L, 2L)
        );

        verifyNoInteractions(
            settlementRepository,
            expenseRepository,
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("이미 LEFT인 모임원은 다시 탈퇴할 수 없고 기존 탈퇴 시각도 유지된다")
    void leaveMeeting_alreadyLeft() {
        // given
        member.leave();

        LocalDateTime originalJoinedAt = member.getJoinedAt();
        LocalDateTime originalLeftAt = member.getLeftAt();

        // when & then
        assertThrows(
            MeetingMemberAlreadyLeftException.class,
            () -> meetingMemberService.leaveMeeting(10L, 2L)
        );

        assertEquals(MeetingMemberStatus.LEFT, member.getStatus());
        assertEquals(originalJoinedAt, member.getJoinedAt());
        assertEquals(originalLeftAt, member.getLeftAt());

        verifyNoInteractions(
            settlementRepository,
            expenseRepository,
            scheduleVoteRepository,
            contentVoteRepository
        );
    }

    @Test
    @DisplayName("참여 중인 모임원은 현재 모임원 목록을 조회할 수 있다")
    void getMeetingMembers_success() {
        // given
        when(meetingRepository.findByIdAndDeletedAtIsNull(10L))
            .thenReturn(Optional.of(meeting));

        when(meetingMemberRepository
            .existsByMeetingIdAndUserIdAndStatus(
                10L,
                2L,
                MeetingMemberStatus.JOINED
            ))
            .thenReturn(true);

        when(meetingMemberRepository.findAllByMeetingIdAndStatus(
            10L,
            MeetingMemberStatus.JOINED
        )).thenReturn(List.of(hostMember, member));

        // when
        var responses =
            meetingMemberService.getMeetingMembers(10L, 2L);

        // then
        assertEquals(2, responses.size());

        assertEquals(100L, responses.get(0).id());
        assertEquals(1L, responses.get(0).userId());
        assertEquals(MeetingMemberStatus.JOINED, responses.get(0).status());

        assertEquals(200L, responses.get(1).id());
        assertEquals(2L, responses.get(1).userId());
        assertEquals("일반 모임원", responses.get(1).nickname());
    }

    @Test
    @DisplayName("현재 참여 중인 모임원이 아니면 모임원 목록을 조회할 수 없다")
    void getMeetingMembers_notJoined() {
        // given
        when(meetingRepository.findByIdAndDeletedAtIsNull(10L))
            .thenReturn(Optional.of(meeting));

        when(meetingMemberRepository
            .existsByMeetingIdAndUserIdAndStatus(
                10L,
                2L,
                MeetingMemberStatus.JOINED
            ))
            .thenReturn(false);

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingMemberService.getMeetingMembers(10L, 2L)
        );

        verify(meetingMemberRepository, never())
            .findAllByMeetingIdAndStatus(
                anyLong(),
                any(MeetingMemberStatus.class)
            );
    }

    @Test
    @DisplayName("존재하지 않거나 삭제된 모임의 모임원 목록은 조회할 수 없다")
    void getMeetingMembers_meetingNotFound() {
        // given
        when(meetingRepository.findByIdAndDeletedAtIsNull(10L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingMemberService.getMeetingMembers(10L, 2L)
        );

        verifyNoInteractions(meetingMemberRepository);
    }
}