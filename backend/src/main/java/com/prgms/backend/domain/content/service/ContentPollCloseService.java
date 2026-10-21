package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.integration.ContentPollClosedEvent;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.content.ContentPollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ContentPollCloseService {

    private final ContentPollRepository contentPollRepository;
    private final MeetingRepository meetingRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 투표판 1건당 트랜잭션 1개. 알림은 AFTER_COMMIT 리스너가 저장
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void closeOneIfExpired(Long pollId) {

        // 투표판이 어느 Meeting 소속인지 확인
        Long meetingId = contentPollRepository
                .findMeetingIdByPollId(pollId)
                .orElseThrow();

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

        ContentPoll poll = contentPollRepository
                .findByIdForUpdate(pollId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));

        LocalDateTime now = LocalDateTime.now();

        // 잠금 이후 재검증 (그 사이 마감되었거나 마감기한이 연장된 경우)
        if (poll.getStatus() != ContentPollStatus.OPEN
                || poll.getDeadline().isAfter(now)) {
            return;
        }

        poll.close();

        eventPublisher.publishEvent(new ContentPollClosedEvent(poll.getMeetingId(), poll.getId()));
    }
}
