package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.service.ContentPollCloseScheduler;
import com.prgms.backend.domain.content.service.ContentPollCloseService;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ContentPollCloseSchedulerTest {

    private final ContentPollRepository repository = mock(ContentPollRepository.class);
    private final ContentPollCloseService closeService = mock(ContentPollCloseService.class);
    private final ContentPollCloseScheduler scheduler = new ContentPollCloseScheduler(repository, closeService);

    @Test
    @DisplayName("한 투표 마감이 실패해도 나머지 투표는 계속 마감")
    void oneFailureDoesNotBlockOthers() {
        // given
        when(repository.findExpiredIds(eq(ContentPollStatus.OPEN), eq(MeetingStatus.ACTIVE), any()))
                .thenReturn(List.of(1L, 2L, 3L));
        doThrow(new IllegalStateException("fail")).when(closeService).closeOneIfExpired(2L);

        // when
        assertDoesNotThrow(scheduler::closeExpired);

        // then
        verify(closeService).closeOneIfExpired(1L);
        verify(closeService).closeOneIfExpired(2L);
        verify(closeService).closeOneIfExpired(3L);
    }
}
