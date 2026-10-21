package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
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
    private final ContentPollCloseService contentPollCloseService;

    @Scheduled(fixedDelay = 30_000) // 30초마다 DB체크, 30초까지 늦게 마감될 수 있음
    public void closeExpired(){
        List<Long> expiredPollIds = contentPollRepository.findExpiredIds(
                ContentPollStatus.OPEN,
                MeetingStatus.ACTIVE,
                LocalDateTime.now()
        );

        for(Long pollId : expiredPollIds){
            try{
                contentPollCloseService.closeOneIfExpired(pollId);
            }catch(RuntimeException e){
                // 한 건의 실패가 다른 투표판의 마감을 막지 않도록, 다음 주기에 재시도
                log.error("콘텐츠 투표판 마감 실패: pollId={}", pollId, e);
            }
        }
    }
}
