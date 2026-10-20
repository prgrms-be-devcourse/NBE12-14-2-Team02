package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
//서버가 켜져있을때 돌다가 마감기한 되면 투표판 닫아줌
public class ContentPollCloseScheduler {
    private final ContentPollRepository contentPollRepository;
    private final ContentPollCloser contentPollCloser;

    // 30초마다 DB체크, 30초까지 늦게 마감될 수 있음
    // 투표마다 별도 트랜잭션이라 한 건이 실패해도 나머지는 마감됨
    @Scheduled(fixedDelay = 30_000)
    public void closeExpired(){
        List<ContentPoll> expired = contentPollRepository
                .findByStatusAndDeadlineLessThanEqual(
                        ContentPollStatus.OPEN,
                        LocalDateTime.now()
                );
        for(ContentPoll poll : expired){
            try{
                contentPollCloser.close(poll.getId());
            }catch(RuntimeException e){
                log.error("콘텐츠 투표 마감 실패: pollId={}", poll.getId(), e);
            }
        }
    }
}
