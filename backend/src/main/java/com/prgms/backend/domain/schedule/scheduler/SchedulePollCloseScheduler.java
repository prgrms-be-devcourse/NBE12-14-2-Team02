package com.prgms.backend.domain.schedule.scheduler;

import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Component
@RequiredArgsConstructor
public class SchedulePollCloseScheduler {
    private final SchedulePollRepository schedulePollRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 30_000) // 30초마다 DB체크, 30초까지 늦게 마감될 수 있음
    @Transactional
    public void closeExpired(){
        List<SchedulePoll> expired = schedulePollRepository
                .findByStatusAndDeadlineLessThanEqual(
                        SchedulePollStatus.OPEN,
                        LocalDateTime.now()
                );
        for(SchedulePoll poll : expired){
            poll.close();
            notificationService.notifyScheduleClosed(poll.getMeeting().getId());
        }
    }

}
