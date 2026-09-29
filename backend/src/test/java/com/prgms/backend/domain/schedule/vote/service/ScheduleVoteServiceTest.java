package com.prgms.backend.domain.schedule.vote.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollClosedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.candidate;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.member;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.poll;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.vote;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScheduleVoteServiceTest {
    private final ScheduleVoteRepository votes = mock(ScheduleVoteRepository.class);
    private final MeetingMemberRepository members = mock(MeetingMemberRepository.class);
    private final ScheduleCandidateRepository candidates = mock(ScheduleCandidateRepository.class);
    private final SchedulePollRepository polls = mock(SchedulePollRepository.class);
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final ScheduleVoteService service =
            new ScheduleVoteService(votes, members, candidates, polls, meetings);

    private SchedulePoll poll;
    private MeetingMember member;
    private ScheduleCandidate candidate;

    @BeforeEach
    void setUp() {
        Meeting meeting = meeting(10L, user(1L, "host"));
        poll = poll(20L, meeting, LocalDateTime.now().plusDays(3));
        member = member(30L, meeting, user(2L, "member"));
        candidate = candidate(40L, poll, LocalDate.now().plusDays(1));
        when(meetings.findByIdAndDeletedAtIsNullForShare(10L)).thenReturn(Optional.of(meeting));
        when(polls.findByMeetingIdForShare(10L)).thenReturn(Optional.of(poll));
        when(members.findByMeetingIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.of(member));
        when(candidates.findByIdAndSchedulePollId(40L, 20L)).thenReturn(Optional.of(candidate));
    }

    @Test
    @DisplayName("첫 응답은 Vote 한 행으로 저장하고 후보·참여자·선호도를 반환한다")
    void submitFirstVote() {
        when(votes.save(any(ScheduleVote.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submit(10L, 40L, 2L,
                new ScheduleVoteRequest.Submit(SchedulePreference.PREFER));

        assertEquals(40L, response.candidateId());
        assertEquals(30L, response.meetingMemberId());
        assertEquals(SchedulePreference.PREFER, response.preference());
        verify(votes).save(any(ScheduleVote.class));
    }

    @Test
    @DisplayName("이미 응답한 후보의 선호도를 바꾸면 기존 Vote를 수정한다")
    void changeExistingVote() {
        ScheduleVote existing = vote(candidate, member, SchedulePreference.PREFER);
        when(votes.findByScheduleCandidateIdAndMeetingMemberId(40L, 30L))
                .thenReturn(Optional.of(existing));

        var response = service.submit(10L, 40L, 2L,
                new ScheduleVoteRequest.Submit(SchedulePreference.IMPOSSIBLE));

        assertEquals(SchedulePreference.IMPOSSIBLE, existing.getPreference());
        assertEquals(SchedulePreference.IMPOSSIBLE, response.preference());
        verify(votes, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 회원은 일정 후보에 응답할 수 없다")
    void rejectLeftMember() {
        member.leave();

        assertThrows(MeetingMemberAlreadyLeftException.class,
                () -> service.submit(10L, 40L, 2L,
                        new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)));
        verifyNoInteractions(candidates, votes);
    }

    @Test
    @DisplayName("다른 Poll의 후보 ID는 현재 모임의 투표 대상으로 인정하지 않는다")
    void rejectCandidateOutsidePoll() {
        when(candidates.findByIdAndSchedulePollId(41L, 20L)).thenReturn(Optional.empty());

        assertThrows(ScheduleCandidateNotFoundException.class,
                () -> service.submit(10L, 41L, 2L,
                        new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)));
        verifyNoInteractions(votes);
    }

    @Test
    @DisplayName("마감된 Poll에는 새 응답이나 선호도 변경을 허용하지 않는다")
    void rejectVoteAfterClose() {
        poll.close();

        assertThrows(SchedulePollClosedException.class,
                () -> service.submit(10L, 40L, 2L,
                        new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)));
        verifyNoInteractions(members, candidates, votes);
    }
}
