package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.integration.ContentPollClosedEvent;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.service.ContentPollCloseService;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ContentPollCloseServiceTest {

    private static final long MEETING_ID = 10L;
    private static final long POLL_ID = 100L;

    private ContentPollRepository contentPollRepository;
    private MeetingRepository meetingRepository;
    private ApplicationEventPublisher eventPublisher;
    private ContentPollCloseService service;

    private Meeting meeting;
    private ContentPoll poll;

    @BeforeEach
    void setUp() {
        contentPollRepository = mock(ContentPollRepository.class);
        meetingRepository = mock(MeetingRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        service = new ContentPollCloseService(contentPollRepository, meetingRepository, eventPublisher);

        meeting = meeting(MEETING_ID, user(1L, "host"));
        poll = poll(LocalDateTime.now().minusMinutes(1));

        when(contentPollRepository.findMeetingIdByPollId(POLL_ID)).thenReturn(Optional.of(MEETING_ID));
        when(meetingRepository.findByIdAndDeletedAtIsNullForShare(MEETING_ID)).thenReturn(Optional.of(meeting));
        when(contentPollRepository.findByIdForUpdate(POLL_ID)).thenReturn(Optional.of(poll));
    }

    private ContentPoll poll(LocalDateTime deadline) {
        ContentPoll contentPoll = new ContentPoll(MEETING_ID, deadline);
        ReflectionTestUtils.setField(contentPoll, "id", POLL_ID);
        return contentPoll;
    }

    @Test
    @DisplayName("마감기한이 지난 OPEN 투표판은 닫고 마감 이벤트를 발행")
    void closeExpiredPoll() {

        // when
        service.closeOneIfExpired(POLL_ID);

        // then
        assertEquals(ContentPollStatus.CLOSED, poll.getStatus());
        verify(eventPublisher).publishEvent(new ContentPollClosedEvent(MEETING_ID, POLL_ID));
    }

    @Test
    @DisplayName("종료된 모임의 투표판은 닫지 않음")
    void skipWhenMeetingCompleted() {

        // given
        meeting.complete();

        // when
        service.closeOneIfExpired(POLL_ID);

        // then
        assertEquals(ContentPollStatus.OPEN, poll.getStatus());
        verify(contentPollRepository, never()).findByIdForUpdate(anyLong());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("이미 닫힌 투표판은 이벤트를 다시 발행하지 않음")
    void skipWhenAlreadyClosed() {

        // given
        poll.close();

        // when
        service.closeOneIfExpired(POLL_ID);

        // then
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("락 획득 후 마감기한이 연장되었으면 닫지 않음")
    void skipWhenDeadlineExtended() {

        // given
        poll.changeDeadLine(LocalDateTime.now().plusDays(1));

        // when
        service.closeOneIfExpired(POLL_ID);

        // then
        assertEquals(ContentPollStatus.OPEN, poll.getStatus());
        verifyNoInteractions(eventPublisher);
    }
}
