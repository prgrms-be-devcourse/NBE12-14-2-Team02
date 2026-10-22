package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentPollRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResultsResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.content.service.ContentPollService;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.content.ContentCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.content.ContentPollAlreadyConfirmedException;
import com.prgms.backend.global.exception.custom.content.ContentPollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.content.ContentPollClosedException;
import com.prgms.backend.global.exception.custom.content.ContentPollNotClosedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.member;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentPollServiceTest {

    private static final long HOST_ID = 1L;
    private static final long MEETING_ID = 10L;
    private static final long POLL_ID = 20L;
    private static final long HOST_MEMBER_ID = 30L;
    private static final long MEMBER_ID = 31L;

    private ContentPollRepository contentPollRepository;
    private MeetingRepository meetingRepository;
    private ContentCandidateRepository contentCandidateRepository;
    private ContentVoteRepository contentVoteRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private ContentPollService service;

    private Meeting meeting;
    private MeetingMember hostMember;
    private MeetingMember member;
    private ContentPoll poll;

    @BeforeEach
    void setUp() {
        contentPollRepository = mock(ContentPollRepository.class);
        meetingRepository = mock(MeetingRepository.class);
        contentCandidateRepository = mock(ContentCandidateRepository.class);
        contentVoteRepository = mock(ContentVoteRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        service = new ContentPollService(contentPollRepository, meetingRepository,
                contentCandidateRepository, contentVoteRepository, meetingMemberRepository);

        meeting = meeting(MEETING_ID, user(HOST_ID, "host"));
        hostMember = member(HOST_MEMBER_ID, meeting, meeting.getHost());
        member = member(MEMBER_ID, meeting, user(2L, "member"));
        poll = poll(LocalDateTime.now().plusDays(1));

        when(meetingRepository.findById(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(meetingRepository.findByIdAndDeletedAtIsNullForShare(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(contentPollRepository.findByMeetingId(MEETING_ID)).thenReturn(Optional.of(poll));
        when(contentPollRepository.findByMeetingIdForUpdate(MEETING_ID)).thenReturn(Optional.of(poll));
        when(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, HOST_ID)).thenReturn(Optional.of(hostMember));
        when(meetingMemberRepository.findByMeetingIdAndUserId(MEETING_ID, 2L)).thenReturn(Optional.of(member));
        when(meetingMemberRepository.findAllByMeetingId(MEETING_ID))
                .thenReturn(List.of(hostMember, member));
    }

    private ContentPoll poll(LocalDateTime deadline) {
        ContentPoll contentPoll = new ContentPoll(MEETING_ID, deadline);
        ReflectionTestUtils.setField(contentPoll, "id", POLL_ID);
        return contentPoll;
    }

    private ContentCandidate candidate(long id, ContentPoll candidatePoll, long createdByMemberId) {
        ContentCandidate candidate = new ContentCandidate(candidatePoll, createdByMemberId, "후보" + id, "");
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }

    @Test
    @DisplayName("호스트는 모임에 콘텐츠 투표판을 생성")
    void createPoll() {

        // given
        LocalDateTime deadline = LocalDateTime.now().plusDays(3);
        when(contentPollRepository.existsByMeetingId(MEETING_ID)).thenReturn(false);
        when(contentPollRepository.save(any(ContentPoll.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        ContentPollResponse.Created created = service.create(MEETING_ID, HOST_ID,
                new ContentPollRequest.Create(deadline));

        // then
        assertEquals(MEETING_ID, created.meetingId());
        assertEquals(deadline, created.deadline());
        assertEquals(ContentPollStatus.OPEN, created.status());
    }

    @Test
    @DisplayName("호스트가 아니면 투표판을 생성할 수 없음")
    void createPollByNonHost() {

        // when / then
        assertThrows(MeetingAccessDeniedException.class, () ->
                service.create(MEETING_ID, 2L, new ContentPollRequest.Create(LocalDateTime.now().plusDays(1))));
        verify(contentPollRepository, never()).save(any());
    }

    @Test
    @DisplayName("이미 투표판이 있는 모임에는 생성할 수 없음")
    void createDuplicatePoll() {

        // given
        when(contentPollRepository.existsByMeetingId(MEETING_ID)).thenReturn(true);

        // when / then
        assertThrows(ContentPollAlreadyExistsException.class, () ->
                service.create(MEETING_ID, HOST_ID, new ContentPollRequest.Create(LocalDateTime.now().plusDays(1))));
    }

    @Test
    @DisplayName("진행 중에는 점수를 숨기고 내 선택만 보여줌")
    void getOpenPollHidesScore() {

        // given
        ContentCandidate candidate = candidate(40L, poll, HOST_MEMBER_ID);
        when(contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(POLL_ID)).thenReturn(List.of(candidate));
        when(contentVoteRepository.findByContentCandidate_ContentPoll_Id(POLL_ID)).thenReturn(List.of(
                new ContentVote(candidate, HOST_MEMBER_ID, ContentPreference.PREFER),
                new ContentVote(candidate, MEMBER_ID, ContentPreference.DISLIKE)
        ));

        // when
        ContentPollResponse.Detail detail = service.get(MEETING_ID, HOST_ID);

        // then
        ContentPollResponse.CandidateRank rank = detail.candidates().get(0);
        assertEquals(0, rank.totalScore());
        assertEquals(ContentPreference.PREFER, rank.myPreference());
    }

    @Test
    @DisplayName("마감 후에는 점수 높은 후보 순으로 정렬")
    void getClosedPollSortsByScore() {

        // given
        poll.close();
        ContentCandidate low = candidate(40L, poll, HOST_MEMBER_ID);
        ContentCandidate high = candidate(41L, poll, MEMBER_ID);
        when(contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(POLL_ID)).thenReturn(List.of(low, high));
        when(contentVoteRepository.findByContentCandidate_ContentPoll_Id(POLL_ID)).thenReturn(List.of(
                new ContentVote(low, HOST_MEMBER_ID, ContentPreference.DISLIKE),
                new ContentVote(high, HOST_MEMBER_ID, ContentPreference.PREFER)
        ));

        // when
        ContentPollResponse.Detail detail = service.get(MEETING_ID, HOST_ID);

        // then
        assertEquals(List.of(41L, 40L),
                detail.candidates().stream().map(ContentPollResponse.CandidateRank::candidateId).toList());
        assertEquals(3, detail.candidates().get(0).totalScore());
    }

    @Test
    @DisplayName("마감기한을 수정하고 모임 공유 잠금 후 투표판 잠금 순서로 조회")
    void updateDeadline() {

        // given
        LocalDateTime newDeadline = LocalDateTime.now().plusDays(5);

        // when
        ContentPollResponse.DeadlineUpdate updated = service.updateDeadline(MEETING_ID, HOST_ID,
                new ContentPollRequest.UpdateDeadline(newDeadline));

        // then
        assertEquals(newDeadline, updated.deadline());
        InOrder order = inOrder(meetingRepository, contentPollRepository);
        order.verify(meetingRepository).findByIdAndDeletedAtIsNullForShare(MEETING_ID);
        order.verify(contentPollRepository).findByMeetingIdForUpdate(MEETING_ID);
    }

    @Test
    @DisplayName("호스트가 아니면 마감기한을 수정할 수 없음")
    void updateDeadlineByNonHost() {

        // when / then
        assertThrows(MeetingAccessDeniedException.class, () -> service.updateDeadline(MEETING_ID, 2L,
                new ContentPollRequest.UpdateDeadline(LocalDateTime.now().plusDays(5))));
        verify(contentPollRepository, never()).findByMeetingIdForUpdate(any());
    }

    @Test
    @DisplayName("이미 마감된 투표판의 마감기한은 수정할 수 없음")
    void updateDeadlineOfClosedPoll() {

        // given
        poll.close();

        // when / then
        assertThrows(ContentPollClosedException.class, () -> service.updateDeadline(MEETING_ID, HOST_ID,
                new ContentPollRequest.UpdateDeadline(LocalDateTime.now().plusDays(5))));
    }

    @Test
    @DisplayName("결과는 SCORE 정렬이면 총점 순, PARTICIPANTS 정렬이면 응답 수 순")
    void getResultsSorted() {

        // given
        poll.close();
        ContentCandidate moreResponses = candidate(40L, poll, HOST_MEMBER_ID);
        ContentCandidate higherScore = candidate(41L, poll, MEMBER_ID);
        when(contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(POLL_ID))
                .thenReturn(List.of(moreResponses, higherScore));
        // 40번: 2명 응답/총점 2, 41번: 1명 응답/총점 3
        when(contentVoteRepository.findByContentCandidate_ContentPoll_Id(POLL_ID)).thenReturn(List.of(
                new ContentVote(moreResponses, HOST_MEMBER_ID, ContentPreference.DISLIKE),
                new ContentVote(moreResponses, MEMBER_ID, ContentPreference.DISLIKE),
                new ContentVote(higherScore, HOST_MEMBER_ID, ContentPreference.PREFER)
        ));

        // when
        ContentPollResultsResponse.Detail byScore =
                service.getResults(MEETING_ID, HOST_ID, ContentPollResultSort.SCORE);
        ContentPollResultsResponse.Detail byParticipants =
                service.getResults(MEETING_ID, HOST_ID, ContentPollResultSort.PARTICIPANTS);

        // then
        assertEquals(List.of(41L, 40L), byScore.candidates().stream()
                .map(ContentPollResultsResponse.CandidateResult::candidateId).toList());
        assertEquals(List.of(40L, 41L), byParticipants.candidates().stream()
                .map(ContentPollResultsResponse.CandidateResult::candidateId).toList());
    }

    @Test
    @DisplayName("결과에 후보별 집계와 미응답 수, 멤버별 선택이 후보 정렬 순서대로 담김")
    void getResultsAggregates() {

        // given
        poll.close();
        ContentCandidate first = candidate(40L, poll, HOST_MEMBER_ID);
        ContentCandidate second = candidate(41L, poll, MEMBER_ID);
        when(contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(POLL_ID))
                .thenReturn(List.of(first, second));
        when(contentVoteRepository.findByContentCandidate_ContentPoll_Id(POLL_ID)).thenReturn(List.of(
                new ContentVote(first, HOST_MEMBER_ID, ContentPreference.PREFER),
                new ContentVote(first, MEMBER_ID, ContentPreference.DISLIKE),
                new ContentVote(second, HOST_MEMBER_ID, ContentPreference.AVAILABLE)
        ));

        // when
        ContentPollResultsResponse.Detail results =
                service.getResults(MEETING_ID, HOST_ID, ContentPollResultSort.SCORE);

        // then
        assertEquals(2, results.joinedCount());
        ContentPollResultsResponse.CandidateResult firstResult = results.candidates().get(0);
        assertEquals(40L, firstResult.candidateId());
        assertEquals(4, firstResult.totalScore());
        assertEquals(1, firstResult.preferCount());
        assertEquals(1, firstResult.dislikeCount());
        assertEquals(0, firstResult.noResponseCount());
        assertEquals("host", firstResult.createdByNickname());
        assertEquals(1, results.candidates().get(1).noResponseCount());

        ContentPollResultsResponse.MemberResult memberResult = results.members().get(1);
        assertEquals(Arrays.asList(ContentPreference.DISLIKE, null), memberResult.preferences());
        assertEquals(1, memberResult.responseCount());
        assertEquals(true, results.members().get(0).host());
    }

    @Test
    @DisplayName("마감 후 탈퇴한 사람의 표는 참여 인원/미응답 수/인원별 결과에 함께 포함되어 합계가 맞음")
    void getResultsKeepsWithdrawnVoter() {

        // given
        poll.close();
        ContentCandidate first = candidate(40L, poll, MEMBER_ID);
        when(contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(POLL_ID))
                .thenReturn(List.of(first));
        when(contentVoteRepository.findByContentCandidate_ContentPoll_Id(POLL_ID)).thenReturn(List.of(
                new ContentVote(first, HOST_MEMBER_ID, ContentPreference.PREFER),
                new ContentVote(first, MEMBER_ID, ContentPreference.DISLIKE)
        ));
        member.leave();

        // when
        ContentPollResultsResponse.Detail results =
                service.getResults(MEETING_ID, HOST_ID, ContentPollResultSort.SCORE);

        // then
        ContentPollResultsResponse.CandidateResult result = results.candidates().get(0);
        assertEquals(2, results.joinedCount());
        assertEquals(2, result.responseCount());
        assertEquals(0, result.noResponseCount());
        assertEquals("member", result.createdByNickname());
        assertEquals(2, results.members().size());
    }

    @Test
    @DisplayName("마감된 투표판의 후보를 확정하고 모임 공유 잠금 후 투표판 잠금 순서로 조회")
    void confirm() {

        // given
        poll.close();
        ContentCandidate candidate = candidate(40L, poll, HOST_MEMBER_ID);
        when(contentCandidateRepository.findById(40L)).thenReturn(Optional.of(candidate));

        // when
        ContentPollResponse.Confirmed confirmed = service.confirm(MEETING_ID, HOST_ID,
                new ContentPollRequest.Confirm(40L));

        // then
        assertEquals(40L, confirmed.confirmedCandidateId());
        InOrder order = inOrder(meetingRepository, contentPollRepository);
        order.verify(meetingRepository).findByIdAndDeletedAtIsNullForShare(MEETING_ID);
        order.verify(contentPollRepository).findByMeetingIdForUpdate(MEETING_ID);
    }

    @Test
    @DisplayName("호스트가 아니면 후보를 확정할 수 없음")
    void confirmByNonHost() {

        // when / then
        assertThrows(MeetingAccessDeniedException.class, () ->
                service.confirm(MEETING_ID, 2L, new ContentPollRequest.Confirm(40L)));
    }

    @Test
    @DisplayName("이미 확정된 투표판은 다시 확정할 수 없음")
    void confirmTwice() {

        // given
        poll.close();
        poll.confirm(40L);

        // when / then
        assertThrows(ContentPollAlreadyConfirmedException.class, () ->
                service.confirm(MEETING_ID, HOST_ID, new ContentPollRequest.Confirm(41L)));
    }

}
