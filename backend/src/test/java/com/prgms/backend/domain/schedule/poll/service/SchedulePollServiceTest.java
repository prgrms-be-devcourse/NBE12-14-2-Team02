package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.poll.dto.SchedulePollRequest;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollClosedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.candidate;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.member;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.poll;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SchedulePollServiceTest {
    private final SchedulePollRepository polls = mock(SchedulePollRepository.class);
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final MeetingMemberRepository members = mock(MeetingMemberRepository.class);
    private final SchedulePollService service = new SchedulePollService(polls, meetings, members);

    private Meeting meeting;
    private SchedulePoll poll;
    private LocalDateTime deadline;

    @BeforeEach
    void setUp() {
        meeting = meeting(10L, user(1L, "host"));
        deadline = LocalDateTime.now().plusDays(3);
        poll = poll(20L, meeting, deadline);
        when(meetings.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(meetings.findByIdAndDeletedAtIsNullForShare(10L)).thenReturn(Optional.of(meeting));
    }

    @Test
    @DisplayName("모임장이 일정 투표를 만들면 OPEN 상태와 마감 시간이 저장된다")
    void createPoll() {
        when(polls.save(any(SchedulePoll.class))).thenAnswer(invocation -> {
            SchedulePoll saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 21L);
            return saved;
        });

        var response = service.create(10L, 1L, new SchedulePollRequest.Create(deadline));

        assertEquals(21L, response.id());
        assertEquals(10L, response.meetingId());
        assertEquals(deadline, response.deadline());
        assertEquals(SchedulePollStatus.OPEN, response.status());
        verify(polls).save(any(SchedulePoll.class));
    }

    @Test
    @DisplayName("이미 일정 투표가 있는 모임에는 두 번째 투표를 만들지 않는다")
    void rejectDuplicatePoll() {
        when(polls.existsByMeetingId(10L)).thenReturn(true);

        assertThrows(SchedulePollAlreadyExistsException.class,
                () -> service.create(10L, 1L, new SchedulePollRequest.Create(deadline)));

        verify(polls, never()).save(any());
    }

    @Test
    @DisplayName("모임장이 아닌 회원은 일정 투표를 만들 수 없다")
    void onlyHostCanCreate() {
        assertThrows(MeetingHostRequiredException.class,
                () -> service.create(10L, 2L, new SchedulePollRequest.Create(deadline)));

        verifyNoInteractions(polls);
    }

    @Test
    @DisplayName("종료된 모임에는 일정 투표를 새로 만들지 않는다")
    void rejectPollForCompletedMeeting() {
        meeting.complete();

        assertThrows(MeetingNotActiveException.class,
                () -> service.create(10L, 1L, new SchedulePollRequest.Create(deadline)));
        verifyNoInteractions(polls);
    }

    @Test
    @DisplayName("참여 중인 회원은 일정 투표와 등록된 후보를 함께 조회한다")
    void joinedMemberReadsPollWithCandidates() {
        MeetingMember member = member(30L, meeting, user(2L, "member"));
        LocalDate candidateDate = LocalDate.now().plusDays(1);
        candidate(40L, poll, candidateDate);
        when(meetings.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(meeting));
        when(members.findByMeetingIdAndUserId(10L, 2L)).thenReturn(Optional.of(member));
        when(polls.findByMeetingIdWithCandidates(10L)).thenReturn(Optional.of(poll));

        var response = service.get(10L, 2L);

        assertEquals(20L, response.id());
        assertEquals(1, response.candidates().size());
        assertEquals(candidateDate, response.candidates().getFirst().candidateDate());
    }

    @Test
    @DisplayName("모임에서 탈퇴한 회원은 일정 투표를 조회할 수 없다")
    void rejectReadByLeftMember() {
        MeetingMember leftMember = member(30L, meeting, user(2L, "member"));
        leftMember.leave();
        when(meetings.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(meeting));
        when(members.findByMeetingIdAndUserId(10L, 2L)).thenReturn(Optional.of(leftMember));

        assertThrows(MeetingMemberAlreadyLeftException.class,
                () -> service.get(10L, 2L));
        verify(polls, never()).findByMeetingIdWithCandidates(10L);
    }

    @Test
    @DisplayName("열린 투표의 마감 시간을 바꾸면 새 시간이 반영된다")
    void updateOpenPollDeadline() {
        LocalDateTime changed = deadline.plusDays(1);
        when(polls.findByMeetingIdForUpdate(10L)).thenReturn(Optional.of(poll));

        var response = service.updateDeadline(10L, 1L,
                new SchedulePollRequest.UpdateDeadline(changed));

        assertEquals(changed, response.deadline());
        assertEquals(SchedulePollStatus.OPEN, response.status());
    }

    @Test
    @DisplayName("마감된 투표는 마감 시간을 변경할 수 없다")
    void rejectDeadlineChangeAfterClose() {
        poll.close();
        when(polls.findByMeetingIdForUpdate(10L)).thenReturn(Optional.of(poll));

        assertThrows(SchedulePollClosedException.class,
                () -> service.updateDeadline(10L, 1L,
                        new SchedulePollRequest.UpdateDeadline(deadline.plusDays(1))));
        assertEquals(deadline, poll.getDeadline());
    }
}
