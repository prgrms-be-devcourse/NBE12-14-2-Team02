package com.prgms.backend.domain.schedule.scheduler;

import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.poll.service.SchedulePollCloseService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SchedulePollCloseSchedulerTest {
    @Test
    void continuesWithNextPollWhenOneCloseFails() {
        SchedulePollRepository polls = mock(SchedulePollRepository.class);
        SchedulePollCloseService closer = mock(SchedulePollCloseService.class);
        when(polls.findExpiredIds(eq(SchedulePollStatus.OPEN), eq(MeetingStatus.ACTIVE),
                any(LocalDateTime.class))).thenReturn(List.of(1L, 2L));
        doThrow(new IllegalStateException("close failure")).when(closer).closeOneIfExpired(1L);
        SchedulePollCloseScheduler scheduler = new SchedulePollCloseScheduler(polls, closer);

        assertDoesNotThrow(scheduler::closeExpired);

        verify(closer).closeOneIfExpired(2L);
    }
}
