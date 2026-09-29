package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotClosedException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ScheduleResultServiceTest {
    private final SchedulePollRepository polls = mock(SchedulePollRepository.class);
    private final MeetingMemberRepository members = mock(MeetingMemberRepository.class);
    private final ScheduleVoteRepository votes = mock(ScheduleVoteRepository.class);
    private final MeetingRepository meetings = mock(MeetingRepository.class);
    private final ScheduleResultService service =
            new ScheduleResultService(polls, members, votes, meetings);

    private SchedulePoll poll;
    private MeetingMember host;

    @BeforeEach
    void setUp() {
        Meeting meeting = meeting(10L, user(1L, "host"));
        poll = poll(20L, meeting, LocalDateTime.now().plusDays(3));
        host = member(30L, meeting, meeting.getHost());
        when(meetings.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(meeting));
        when(members.findByMeetingIdAndUserId(10L, 1L)).thenReturn(Optional.of(host));
        when(polls.findByMeetingId(10L)).thenReturn(Optional.of(poll));
    }

    @Test
    @DisplayName("마감 결과는 총점·공동 순위·0점 응답·미응답을 구분하고 탈퇴자 표를 제외한다")
    void rankClosedPoll() {
        poll.close();
        LocalDate firstDate = LocalDate.now().plusDays(1);
        ScheduleCandidate first = candidate(40L, poll, firstDate);
        ScheduleCandidate second = candidate(41L, poll, firstDate.plusDays(1));
        ScheduleCandidate unanswered = candidate(42L, poll, firstDate.plusDays(2));
        Meeting meeting = poll.getMeeting();
        MeetingMember secondMember = member(31L, meeting, user(2L, "second"));
        MeetingMember thirdMember = member(32L, meeting, user(3L, "third"));
        MeetingMember leftMember = member(33L, meeting, user(4L, "left"));
        leftMember.leave();

        when(members.findAllByMeetingIdAndStatus(10L, MeetingMemberStatus.JOINED))
                .thenReturn(List.of(host, secondMember, thirdMember));
        when(votes.findAllByScheduleCandidateSchedulePollId(20L)).thenReturn(List.of(
                vote(first, host, SchedulePreference.PREFER),
                vote(first, secondMember, SchedulePreference.IMPOSSIBLE),
                vote(second, host, SchedulePreference.AVAILABLE),
                vote(second, secondMember, SchedulePreference.DISLIKE),
                vote(unanswered, leftMember, SchedulePreference.PREFER)
        ));

        var result = service.getResults(10L, 1L);

        assertEquals(List.of(40L, 41L, 42L), result.candidateRanks().stream()
                .map(rank -> rank.candidateId()).toList());
        assertEquals(List.of(1, 1, 3), result.candidateRanks().stream()
                .map(rank -> rank.rank()).toList());
        assertEquals(List.of(3, 3, 0), result.candidateRanks().stream()
                .map(rank -> rank.totalScore()).toList());
        assertEquals(2, result.candidateRanks().getFirst().responseCount());
        assertEquals(1, result.candidateRanks().getFirst().nonResponseCount());
        assertEquals(1, result.candidateRanks().getFirst().preferenceCounts().impossible());
        assertEquals(0, result.candidateRanks().getLast().responseCount());
        assertEquals(3, result.candidateRanks().getLast().nonResponseCount());

        assertEquals(3, result.participantResponses().size());
        assertEquals(SchedulePreference.PREFER,
                result.participantResponses().getFirst().answers().getFirst().preference());
        assertEquals(SchedulePreference.IMPOSSIBLE,
                result.participantResponses().get(1).answers().getFirst().preference());
        assertNull(result.participantResponses().getLast().answers().getFirst().preference());
    }

    @Test
    @DisplayName("열린 투표에서는 순위와 참여자별 결과를 공개하지 않는다")
    void rejectResultsBeforeClose() {
        assertThrows(SchedulePollNotClosedException.class,
                () -> service.getResults(10L, 1L));
        verifyNoInteractions(votes);
    }
}
