package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentVoteRequest;
import com.prgms.backend.domain.content.dto.response.ContentVoteResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.content.service.ContentVoteService;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.global.exception.custom.content.ContentCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.content.ContentPollClosedException;
import com.prgms.backend.global.exception.custom.content.ContentPollNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.member;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentVoteServiceTest {

    private static final long MEETING_ID = 10L;
    private static final long POLL_ID = 20L;
    private static final long MEMBER_ID = 30L;
    private static final long CANDIDATE_ID = 40L;
    private static final long USER_ID = 1L;

    private ContentVoteRepository contentVoteRepository;
    private ContentCandidateRepository contentCandidateRepository;
    private ContentPollRepository contentPollRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private ContentVoteService service;

    private MeetingMember member;
    private ContentPoll poll;
    private ContentCandidate candidate;

    @BeforeEach
    void setUp() {
        contentVoteRepository = mock(ContentVoteRepository.class);
        contentCandidateRepository = mock(ContentCandidateRepository.class);
        contentPollRepository = mock(ContentPollRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        service = new ContentVoteService(contentVoteRepository, contentCandidateRepository,
                contentPollRepository, meetingMemberRepository);

        Meeting meeting = meeting(MEETING_ID, user(USER_ID, "host"));
        member = member(MEMBER_ID, meeting, meeting.getHost());
        poll = poll(POLL_ID);
        candidate = candidate(CANDIDATE_ID, poll);

        when(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(member));
        when(contentPollRepository.findByMeetingId(MEETING_ID)).thenReturn(Optional.of(poll));
        when(contentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(contentVoteRepository.findByContentCandidateIdAndMeetingMemberId(CANDIDATE_ID, MEMBER_ID))
                .thenReturn(Optional.empty());
        when(contentVoteRepository.save(any(ContentVote.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ContentPoll poll(long id) {
        ContentPoll contentPoll = new ContentPoll(MEETING_ID, LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(contentPoll, "id", id);
        return contentPoll;
    }

    private ContentCandidate candidate(long id, ContentPoll candidatePoll) {
        ContentCandidate contentCandidate = new ContentCandidate(candidatePoll, MEMBER_ID, "후보", "설명");
        ReflectionTestUtils.setField(contentCandidate, "id", id);
        return contentCandidate;
    }

    @Test
    @DisplayName("처음 투표하면 새 투표를 저장")
    void createVote() {

        // when
        ContentVoteResponse.Saved saved = service.upsert(MEETING_ID, USER_ID,
                new ContentVoteRequest.Submit(CANDIDATE_ID, ContentPreference.PREFER));

        // then
        assertEquals(CANDIDATE_ID, saved.candidateId());
        assertEquals(MEMBER_ID, saved.meetingMemberId());
        assertEquals(ContentPreference.PREFER, saved.preference());
        verify(contentVoteRepository).save(any(ContentVote.class));
    }

    @Test
    @DisplayName("이미 투표했으면 새로 저장하지 않고 선호도만 변경")
    void changeExistingVote() {

        // given
        ContentVote existing = new ContentVote(candidate, MEMBER_ID, ContentPreference.PREFER);
        when(contentVoteRepository.findByContentCandidateIdAndMeetingMemberId(CANDIDATE_ID, MEMBER_ID))
                .thenReturn(Optional.of(existing));

        // when
        ContentVoteResponse.Saved saved = service.upsert(MEETING_ID, USER_ID,
                new ContentVoteRequest.Submit(CANDIDATE_ID, ContentPreference.DISLIKE));

        // then
        assertEquals(ContentPreference.DISLIKE, saved.preference());
        assertEquals(ContentPreference.DISLIKE, existing.getPreference());
        verify(contentVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 멤버는 투표할 수 없음")
    void voteByLeftMember() {

        // given
        member.leave();

        // when / then
        assertThrows(MeetingAccessDeniedException.class, () -> service.upsert(MEETING_ID, USER_ID,
                new ContentVoteRequest.Submit(CANDIDATE_ID, ContentPreference.PREFER)));
    }

    @Test
    @DisplayName("마감된 투표판에는 투표할 수 없음")
    void voteInClosedPoll() {

        // given
        poll.close();

        // when / then
        assertThrows(ContentPollClosedException.class, () -> service.upsert(MEETING_ID, USER_ID,
                new ContentVoteRequest.Submit(CANDIDATE_ID, ContentPreference.PREFER)));
        verify(contentVoteRepository, never()).save(any());
    }

    @Test
    @DisplayName("다른 투표판의 후보에는 투표할 수 없음")
    void voteForCandidateOfOtherPoll() {

        // given
        when(contentCandidateRepository.findById(CANDIDATE_ID))
                .thenReturn(Optional.of(candidate(CANDIDATE_ID, poll(999L))));

        // when / then
        assertThrows(ContentCandidateNotFoundException.class, () -> service.upsert(MEETING_ID, USER_ID,
                new ContentVoteRequest.Submit(CANDIDATE_ID, ContentPreference.PREFER)));
        verify(contentVoteRepository, never()).save(any());
    }
}
