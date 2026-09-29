package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
import com.prgms.backend.global.exception.custom.schedule.DuplicateScheduleCandidateException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateHasVotesException;
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
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.poll;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScheduleCandidateServiceTest {
    private final ScheduleCandidateRepository candidates = mock(ScheduleCandidateRepository.class);
    private final SchedulePollRepository polls = mock(SchedulePollRepository.class);
    private final ScheduleVoteRepository votes = mock(ScheduleVoteRepository.class);
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final ScheduleCandidateService service =
            new ScheduleCandidateService(candidates, polls, votes, meetings);

    private SchedulePoll poll;
    private LocalDate firstDate;

    @BeforeEach
    void setUp() {
        Meeting meeting = meeting(10L, user(1L, "host"));
        poll = poll(20L, meeting, LocalDateTime.now().plusDays(3));
        firstDate = LocalDate.now().plusDays(1);
        when(meetings.findByIdAndDeletedAtIsNullForShare(10L)).thenReturn(Optional.of(meeting));
        when(polls.findByMeetingIdForUpdate(10L)).thenReturn(Optional.of(poll));
    }

    @Test
    @DisplayName("모임장이 후보를 추가하면 Poll 목록과 응답에 새 후보가 반영된다")
    void createCandidate() {
        when(candidates.save(any(ScheduleCandidate.class))).thenAnswer(invocation -> {
            ScheduleCandidate saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 40L);
            return saved;
        });

        var response = service.create(10L, 1L, new ScheduleCandidateRequest.Create(firstDate));

        assertEquals(40L, response.id());
        assertEquals(firstDate, response.candidateDate());
        assertEquals(1, poll.getCandidates().size());
        assertEquals(poll, poll.getCandidates().getFirst().getSchedulePoll());
    }

    @Test
    @DisplayName("같은 투표에 이미 있는 날짜는 후보로 다시 추가하지 않는다")
    void rejectDuplicateDate() {
        candidate(40L, poll, firstDate);

        assertThrows(DuplicateScheduleCandidateException.class,
                () -> service.create(10L, 1L, new ScheduleCandidateRequest.Create(firstDate)));

        assertEquals(1, poll.getCandidates().size());
        verify(candidates, never()).save(any());
    }

    @Test
    @DisplayName("응답이 없는 후보는 다른 날짜로 수정하고 삭제할 수 있다")
    void updateAndDeleteUnvotedCandidate() {
        ScheduleCandidate candidate = candidate(40L, poll, firstDate);
        LocalDate changed = firstDate.plusDays(1);

        var response = service.update(10L, 40L, 1L, new ScheduleCandidateRequest.Update(changed));
        assertEquals(changed, response.candidateDate());
        assertEquals(changed, candidate.getCandidateDate());

        service.delete(10L, 40L, 1L);
        assertTrue(poll.getCandidates().isEmpty());
    }

    @Test
    @DisplayName("다른 후보와 날짜가 같아지는 수정은 거부한다")
    void rejectDuplicateDateOnUpdate() {
        ScheduleCandidate first = candidate(40L, poll, firstDate);
        LocalDate occupied = firstDate.plusDays(1);
        candidate(41L, poll, occupied);

        assertThrows(DuplicateScheduleCandidateException.class,
                () -> service.update(10L, 40L, 1L, new ScheduleCandidateRequest.Update(occupied)));
        assertEquals(firstDate, first.getCandidateDate());
    }

    @Test
    @DisplayName("응답이 있는 후보는 삭제할 수 없다")
    void rejectDeleteAfterVote() {
        candidate(40L, poll, firstDate);
        when(votes.existsByScheduleCandidateId(40L)).thenReturn(true);

        assertThrows(ScheduleCandidateHasVotesException.class,
                () -> service.delete(10L, 40L, 1L));

        assertEquals(1, poll.getCandidates().size());
    }

    @Test
    @DisplayName("모임장이 아닌 회원의 요청은 Poll 잠금 조회 전에 거부한다")
    void rejectNonHost() {
        assertThrows(MeetingHostRequiredException.class,
                () -> service.create(10L, 2L, new ScheduleCandidateRequest.Create(firstDate)));

        verify(polls, never()).findByMeetingIdForUpdate(any());
    }

    @Test
    @DisplayName("마감된 투표에는 새 후보를 추가할 수 없다")
    void rejectCandidateAfterClose() {
        poll.close();

        assertThrows(SchedulePollClosedException.class,
                () -> service.create(10L, 1L, new ScheduleCandidateRequest.Create(firstDate)));
        verify(candidates, never()).save(any());
    }
}
