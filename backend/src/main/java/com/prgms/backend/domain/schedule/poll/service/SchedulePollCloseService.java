package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
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
    private final MeetingRepository meetingRepository;

    @Transactional
    public void closeOneIfExpired(Long pollId) {

        // Poll이 어느 Meeting 소속인지 확인
        Long meetingId = schedulePollRepository
                .findMeetingIdByPollId(pollId)
                .orElse(null);

        if (meetingId == null) {
            return;
        }

        // Meeting을 먼저 공유 잠금
        Meeting meeting = meetingRepository
                .findByIdAndDeletedAtIsNullForShare(meetingId)
                .orElse(null);

        // 삭제된 모임
        if (meeting == null) {
            return;
        }

        // 이미 종료된 모임
        if (!meeting.isActive()) {
            return;
        }

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