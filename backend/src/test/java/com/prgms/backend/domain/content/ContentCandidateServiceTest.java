package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.dto.request.ContentCandidateRequest;
import com.prgms.backend.domain.content.dto.response.ContentCandidateResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.content.service.ContentCandidateService;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.global.exception.custom.content.ContentCandidateHasVotesException;
import com.prgms.backend.global.exception.custom.content.ContentCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.content.ContentPollClosedException;
import com.prgms.backend.global.exception.custom.content.ContentPollNotFoundException;
import com.prgms.backend.global.exception.custom.content.DuplicateContentCandidateTitleException;
import com.prgms.backend.global.exception.custom.content.NotContentCandidateOwnerException;
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

class ContentCandidateServiceTest {

    private static final long MEETING_ID = 10L;
    private static final long POLL_ID = 20L;
    private static final long MEMBER_ID = 30L;
    private static final long OTHER_MEMBER_ID = 31L;
    private static final long CANDIDATE_ID = 40L;
    private static final long USER_ID = 1L;

    private ContentCandidateRepository contentCandidateRepository;
    private ContentPollRepository contentPollRepository;
    private ContentVoteRepository contentVoteRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private ContentCandidateService service;

    private MeetingMember member;
    private ContentPoll poll;
    private ContentCandidate candidate;

    @BeforeEach
    void setUp() {
        contentCandidateRepository = mock(ContentCandidateRepository.class);
        contentPollRepository = mock(ContentPollRepository.class);
        contentVoteRepository = mock(ContentVoteRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        service = new ContentCandidateService(contentCandidateRepository, contentPollRepository,
                contentVoteRepository, meetingMemberRepository);

        Meeting meeting = meeting(MEETING_ID, user(USER_ID, "host"));
        member = member(MEMBER_ID, meeting, meeting.getHost());
        poll = poll(POLL_ID);
        candidate = candidate(CANDIDATE_ID, poll, MEMBER_ID, "기존 후보");

        when(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.of(member));
        when(contentPollRepository.findByMeetingId(MEETING_ID)).thenReturn(Optional.of(poll));
        when(contentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(candidate));
        when(contentCandidateRepository.save(any(ContentCandidate.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private ContentPoll poll(long id) {
        ContentPoll contentPoll = new ContentPoll(MEETING_ID, LocalDateTime.now().plusDays(1));
        ReflectionTestUtils.setField(contentPoll, "id", id);
        return contentPoll;
    }

    private ContentCandidate candidate(long id, ContentPoll candidatePoll, long createdByMemberId, String title) {
        ContentCandidate contentCandidate = new ContentCandidate(candidatePoll, createdByMemberId, title, "설명");
        ReflectionTestUtils.setField(contentCandidate, "id", id);
        return contentCandidate;
    }

    @Test
    @DisplayName("참여 중인 멤버는 후보를 생성")
    void createCandidate() {

        // given
        when(contentCandidateRepository.existsByContentPollIdAndTitle(POLL_ID, "새 후보")).thenReturn(false);

        // when
        ContentCandidateResponse.Saved saved = service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("새 후보", "설명"));

        // then
        assertEquals("새 후보", saved.title());
        assertEquals(MEMBER_ID, saved.createdByMemberId());
        assertEquals(POLL_ID, saved.contentPollId());
    }

    @Test
    @DisplayName("모임 멤버가 아니면 후보를 생성할 수 없음")
    void createByNonMember() {

        // given
        when(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, USER_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(MeetingMemberNotFoundException.class, () -> service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("새 후보", "설명")));
        verify(contentCandidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("탈퇴한 멤버는 후보를 생성할 수 없음")
    void createByLeftMember() {

        // given
        member.leave();

        // when / then
        assertThrows(MeetingAccessDeniedException.class, () -> service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("새 후보", "설명")));
    }

    @Test
    @DisplayName("투표판이 없으면 후보를 생성할 수 없음")
    void createWithoutPoll() {

        // given
        when(contentPollRepository.findByMeetingId(MEETING_ID)).thenReturn(Optional.empty());

        // when / then
        assertThrows(ContentPollNotFoundException.class, () -> service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("새 후보", "설명")));
    }

    @Test
    @DisplayName("마감된 투표판에는 후보를 생성할 수 없음")
    void createInClosedPoll() {

        // given
        poll.close();

        // when / then
        assertThrows(ContentPollClosedException.class, () -> service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("새 후보", "설명")));
    }

    @Test
    @DisplayName("같은 투표판에 같은 제목의 후보는 생성할 수 없음")
    void createDuplicateTitle() {

        // given
        when(contentCandidateRepository.existsByContentPollIdAndTitle(POLL_ID, "기존 후보")).thenReturn(true);

        // when / then
        assertThrows(DuplicateContentCandidateTitleException.class, () -> service.create(MEETING_ID, USER_ID,
                new ContentCandidateRequest.Create("기존 후보", "설명")));
        verify(contentCandidateRepository, never()).save(any());
    }

    @Test
    @DisplayName("후보를 만든 멤버는 투표가 없으면 후보를 수정")
    void updateCandidate() {

        // when
        ContentCandidateResponse.Saved saved = service.update(MEETING_ID, CANDIDATE_ID, USER_ID,
                new ContentCandidateRequest.Update("수정된 후보", "수정된 설명"));

        // then
        assertEquals("수정된 후보", saved.title());
        assertEquals("수정된 설명", candidate.getDescription());
    }

    @Test
    @DisplayName("후보를 만든 멤버가 아니면 수정할 수 없음")
    void updateByNonOwner() {

        // given
        ContentCandidate othersCandidate = candidate(CANDIDATE_ID, poll, OTHER_MEMBER_ID, "남의 후보");
        when(contentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(othersCandidate));

        // when / then
        assertThrows(NotContentCandidateOwnerException.class, () -> service.update(MEETING_ID, CANDIDATE_ID,
                USER_ID, new ContentCandidateRequest.Update("수정", "설명")));
    }

    @Test
    @DisplayName("이미 투표가 달린 후보는 수정할 수 없음")
    void updateCandidateWithVotes() {

        // given
        when(contentVoteRepository.existsByContentCandidateId(CANDIDATE_ID)).thenReturn(true);

        // when / then
        assertThrows(ContentCandidateHasVotesException.class, () -> service.update(MEETING_ID, CANDIDATE_ID,
                USER_ID, new ContentCandidateRequest.Update("수정", "설명")));
        assertEquals("기존 후보", candidate.getTitle());
    }

    @Test
    @DisplayName("다른 후보와 제목이 겹치게 수정할 수 없음")
    void updateDuplicateTitle() {

        // given
        when(contentCandidateRepository.existsByContentPollIdAndTitleAndIdNot(POLL_ID, "중복 제목", CANDIDATE_ID))
                .thenReturn(true);

        // when / then
        assertThrows(DuplicateContentCandidateTitleException.class, () -> service.update(MEETING_ID,
                CANDIDATE_ID, USER_ID, new ContentCandidateRequest.Update("중복 제목", "설명")));
        assertEquals("기존 후보", candidate.getTitle());
    }

    @Test
    @DisplayName("다른 투표판의 후보는 수정할 수 없음")
    void updateCandidateOfOtherPoll() {

        // given
        ContentCandidate otherPollCandidate = candidate(CANDIDATE_ID, poll(999L), MEMBER_ID, "다른 판 후보");
        when(contentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(otherPollCandidate));

        // when / then
        assertThrows(ContentCandidateNotFoundException.class, () -> service.update(MEETING_ID, CANDIDATE_ID,
                USER_ID, new ContentCandidateRequest.Update("수정", "설명")));
    }

    @Test
    @DisplayName("후보를 만든 멤버는 투표가 없으면 후보를 삭제")
    void deleteCandidate() {

        // when
        service.delete(MEETING_ID, CANDIDATE_ID, USER_ID);

        // then
        verify(contentCandidateRepository).delete(candidate);
    }

    @Test
    @DisplayName("후보를 만든 멤버가 아니면 삭제할 수 없음")
    void deleteByNonOwner() {

        // given
        ContentCandidate othersCandidate = candidate(CANDIDATE_ID, poll, OTHER_MEMBER_ID, "남의 후보");
        when(contentCandidateRepository.findById(CANDIDATE_ID)).thenReturn(Optional.of(othersCandidate));

        // when / then
        assertThrows(NotContentCandidateOwnerException.class, () -> service.delete(MEETING_ID, CANDIDATE_ID, USER_ID));
        verify(contentCandidateRepository, never()).delete(any());
    }

    @Test
    @DisplayName("이미 투표가 달린 후보는 삭제할 수 없음")
    void deleteCandidateWithVotes() {

        // given
        when(contentVoteRepository.existsByContentCandidateId(CANDIDATE_ID)).thenReturn(true);

        // when / then
        assertThrows(ContentCandidateHasVotesException.class, () -> service.delete(MEETING_ID, CANDIDATE_ID, USER_ID));
        verify(contentCandidateRepository, never()).delete(any());
    }
}
