package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.event.SchedulePollClosedEvent;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SchedulePollCloseService {

    private final SchedulePollRepository schedulePollRepository;
    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // meetingId 조회가 먼저 Read View를 만들더라도 알림 대상은 마감 시점의 커밋된 참여자로 조회한다.
    @Transactional(isolation = Isolation.READ_COMMITTED)
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

        //투표소를 잠근다. 더 이상 투표할 수 없고, 후보를 추가할 수도 없음
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

        //투표소 마감.
        poll.close();

        eventPublisher.publishEvent(
                new SchedulePollClosedEvent(poll.getId(), meetingId)
        );
    }
}
