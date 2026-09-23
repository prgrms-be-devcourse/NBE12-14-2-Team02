package com.prgms.backend.domain.schedule.scheduler;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.poll.service.SchedulePollCloseService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
public class SchedulePollCloseScheduler {

    private final SchedulePollRepository schedulePollRepository;
    private final SchedulePollCloseService schedulePollCloseService;

    @Scheduled(fixedDelay = 30_000)
    public void closeExpired() {
        List<Long> expiredPollIds =
                schedulePollRepository.findExpiredIds(
                        SchedulePollStatus.OPEN,
                        LocalDateTime.now()
                );

        for (Long pollId : expiredPollIds) {
            schedulePollCloseService.closeOneIfExpired(pollId);
        }
    }
}
