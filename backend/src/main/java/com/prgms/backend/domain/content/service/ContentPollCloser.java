package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.integration.ContentPollClosedEvent;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
// 투표 한 건을 독립된 트랜잭션으로 닫는다. 한 건이 실패해도 다른 투표 마감에 영향이 없도록 분리
public class ContentPollCloser {
    private final ContentPollRepository contentPollRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void close(Long pollId){
        // 조회 이후 마감기한이 연장되었거나 이미 닫혔을 수 있으므로 다시 확인
        ContentPoll poll = contentPollRepository.findById(pollId).orElse(null);
        if(poll == null
                || poll.getStatus() != ContentPollStatus.OPEN
                || poll.getDeadline().isAfter(LocalDateTime.now())){
            return;
        }
        poll.close();
        eventPublisher.publishEvent(new ContentPollClosedEvent(poll.getMeetingId(), poll.getId()));
    }
}
