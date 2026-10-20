package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.integration.ContentPollClosedEvent;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.service.ContentPollCloseScheduler;
import com.prgms.backend.domain.content.service.ContentPollCloser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContentPollCloseSchedulerTest {

    private final ContentPollRepository repository = mock(ContentPollRepository.class);
    private final ContentPollCloser closer = mock(ContentPollCloser.class);
    private final ContentPollCloseScheduler scheduler = new ContentPollCloseScheduler(repository, closer);

    private ContentPoll poll(long id, long meetingId, LocalDateTime deadline) {
        ContentPoll poll = new ContentPoll(meetingId, deadline);
        ReflectionTestUtils.setField(poll, "id", id);
        return poll;
    }

    @Test
    @DisplayName("한 투표 마감이 실패해도 나머지 투표는 계속 마감")
    void oneFailureDoesNotBlockOthers() {
        // given
        LocalDateTime past = LocalDateTime.now().minusMinutes(1);
        when(repository.findByStatusAndDeadlineLessThanEqual(eq(ContentPollStatus.OPEN), any()))
                .thenReturn(List.of(poll(1L, 10L, past), poll(2L, 20L, past), poll(3L, 30L, past)));
        doThrow(new IllegalStateException("fail")).when(closer).close(2L);

        // when
        assertDoesNotThrow(scheduler::closeExpired);

        // then
        verify(closer).close(1L);
        verify(closer).close(2L);
        verify(closer).close(3L);
    }

    @Test
    @DisplayName("Closer는 마감기한이 지난 OPEN 투표만 닫고 이벤트를 발행")
    void closerClosesExpiredPollAndPublishesEvent() {
        // given
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ContentPollCloser realCloser = new ContentPollCloser(repository, publisher);
        ContentPoll expired = poll(1L, 10L, LocalDateTime.now().minusMinutes(1));
        when(repository.findById(1L)).thenReturn(Optional.of(expired));

        // when
        realCloser.close(1L);

        // then
        assertEquals(ContentPollStatus.CLOSED, expired.getStatus());
        verify(publisher).publishEvent(new ContentPollClosedEvent(10L, 1L));
    }

    @Test
    @DisplayName("Closer는 조회 이후 마감기한이 연장된 투표는 닫지 않음")
    void closerSkipsExtendedPoll() {
        // given
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ContentPollCloser realCloser = new ContentPollCloser(repository, publisher);
        ContentPoll extended = poll(1L, 10L, LocalDateTime.now().plusHours(1));
        when(repository.findById(1L)).thenReturn(Optional.of(extended));

        // when
        realCloser.close(1L);

        // then
        assertEquals(ContentPollStatus.OPEN, extended.getStatus());
        verifyNoInteractions(publisher);
    }

    @Test
    @DisplayName("Closer는 이미 닫힌 투표는 이벤트를 다시 발행하지 않음")
    void closerSkipsAlreadyClosedPoll() {
        // given
        ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
        ContentPollCloser realCloser = new ContentPollCloser(repository, publisher);
        ContentPoll closed = poll(1L, 10L, LocalDateTime.now().minusMinutes(1));
        closed.close();
        when(repository.findById(1L)).thenReturn(Optional.of(closed));

        // when
        realCloser.close(1L);

        // then
        verifyNoInteractions(publisher);
    }
}
