package com.prgms.backend.domain.content.integration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ContentPollClosedEventListener {
    private final ContentNotificationWriter contentNotificationWriter;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onClosed(ContentPollClosedEvent event){
        try{
            contentNotificationWriter.save(event.meetingId());
        }catch(RuntimeException e){
            log.error("콘텐츠 마감 알림 저장 실패: meetingId={}, pollId={}", event.meetingId(), event.pollId(), e);
        }
    }
}
