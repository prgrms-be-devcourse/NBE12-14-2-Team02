package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SchedulePollCloseService {

    private final SchedulePollRepository schedulePollRepository;
    private final NotificationService notificationService;

    @Transactional
    public void closeOneIfExpired(Long pollId) {
        SchedulePoll poll = schedulePollRepository
                .findByIdForUpdate(pollId)
                .orElse(null);

        if (poll == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        if (poll.getStatus() != SchedulePollStatus.OPEN
                || poll.getDeadline().isAfter(now)) {
            return;
        }

        poll.close();

        notificationService.notifyScheduleClosed(
                poll.getMeeting().getId()
        );
    }
}