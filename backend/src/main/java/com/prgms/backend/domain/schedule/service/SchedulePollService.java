package com.prgms.backend.domain.schedule.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.dto.SchedulePollCreateRequest;
import com.prgms.backend.domain.schedule.dto.SchedulePollResponse;
import com.prgms.backend.domain.schedule.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.repository.SchedulePollRepository;
import com.prgms.backend.global.exception.custom.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.SchedulePollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SchedulePollService {
    private final SchedulePollRepository schedulePollRepository;
    private final MeetingRepository meetingRepository;

    //schedulePoll생성
    @Transactional
    public SchedulePollResponse create(
            Long meetingId,
            SchedulePollCreateRequest request
    ) {
       Meeting meeting = meetingRepository.findById(meetingId)
               .orElseThrow(
                       () ->
                               new MeetingNotFoundException(meetingId)
               );
       //이미 모임에 일정 투표가 등록되어있다면, 예외
       if(schedulePollRepository.existsByMeetingId(meetingId)){
           throw new SchedulePollAlreadyExistsException(meetingId);
       }

       SchedulePoll schedulePoll =
               SchedulePoll.create(meeting,request.deadline());
       //request에 있는 후보 일정들을 투표에다가 add
       request.candidateDates()
               .forEach(schedulePoll::addCandidate);

       //이렇게 하면, 후보일정까지 한 트랜잭션 내에서 DB에 반영
       SchedulePoll savedPoll =
               schedulePollRepository.save(schedulePoll);

       //DTO로 변환해서 반환
       return SchedulePollResponse.from(savedPoll);
    }

    //SchedulePoll 조회하기
    @Transactional(readOnly = true)
    public SchedulePollResponse get(Long meetingId) {
        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingId(meetingId)
                .orElseThrow(
                        () -> new SchedulePollNotFoundException(
                                meetingId
                        )
                );
        return SchedulePollResponse.from(schedulePoll);



    }
}