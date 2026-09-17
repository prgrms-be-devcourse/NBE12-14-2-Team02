package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
//서버가 켜져있을때 돌다가 마감기한 되면 투표판 닫아줌
public class ContentPollCloseScheduler {
    private final ContentPollRepository contentPollRepository;

    @Scheduled(fixedDelay = 30_000) // 30초마다 DB체크, 30초까지 늦게 마감될 수 있음
    @Transactional
    public void closeExpired(){
        List<ContentPoll> expired = contentPollRepository
                .findByStatusAndDeadlineLessThanEqual(
                        ContentPollStatus.OPEN,
                        LocalDateTime.now()
                );
        expired.forEach(ContentPoll::close);
    }
}
