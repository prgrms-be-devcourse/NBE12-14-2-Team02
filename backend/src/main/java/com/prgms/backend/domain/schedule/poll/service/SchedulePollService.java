package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.poll.dto.SchedulePollRequest;
import com.prgms.backend.domain.schedule.poll.dto.SchedulePollResponse;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class SchedulePollService {
    private final SchedulePollRepository schedulePollRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    //schedulePoll생성
    @Transactional
    public SchedulePollResponse.Created create(
            Long meetingId,
            Long userId,
            SchedulePollRequest.Create request
    ) {

       Meeting meeting = meetingRepository.findById(meetingId)
               .orElseThrow(
                       () ->
                               new MeetingNotFoundException(meetingId)
               );
        //요청하는 애가 호스트임? 맞다면, 통과.
       validateHost(meeting,userId);

       //이미 모임에 일정 투표가 등록되어있다면, 예외
       if(schedulePollRepository.existsByMeetingId(meetingId)){
           throw new SchedulePollAlreadyExistsException(meetingId);
       }

       SchedulePoll savedPoll =
               schedulePollRepository.save(SchedulePoll.create(meeting,request.deadline()));

       //DTO로 변환해서 반환
       return SchedulePollResponse.Created.from(savedPoll);
    }

    //SchedulePoll 조회하기
    @Transactional(readOnly = true)
    public SchedulePollResponse.Detail get(Long meetingId, Long userId) {



        MeetingMember meetingMember =
                meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                        .orElseThrow(
                                () -> new MeetingMemberNotFoundException(
                                        meetingId,
                                        userId
                                )
                        );

        //탈퇴한 회원이라면 예외
        if(!meetingMember.isJoined()){
            throw new MeetingMemberAlreadyLeftException(meetingId, userId);
        }

        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingId(meetingId)
                .orElseThrow(
                        () -> new SchedulePollNotFoundException(
                                meetingId
                        )
                );
        return SchedulePollResponse.Detail.from(schedulePoll);



    }

    @Transactional
    public SchedulePollResponse.DeadlineUpdate updateDeadline(
            Long meetingId,
            Long userId,
            SchedulePollRequest.UpdateDeadline request
            ) {

        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(
                        () ->
                                new MeetingNotFoundException(meetingId)
                );
        //요청하는 애가 호스트임? 맞다면, 통과.
        validateHost(meeting,userId);

        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingId(meetingId)
                .orElseThrow(
                        () -> new SchedulePollNotFoundException(
                                meetingId
                        )
                );
        schedulePoll.updateDeadline(request.deadline(),LocalDateTime.now());

        return SchedulePollResponse.DeadlineUpdate.from(
                schedulePoll
        );

    }

    //호스트인지 아닌지 검증하는 메서드
    private void validateHost(
            Meeting meeting,
            Long userId
    ) {
        if (!meeting.isHost(userId)) {
            throw new MeetingHostRequiredException();
        }
    }

}