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
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
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

    @Transactional
    public SchedulePollResponse.Created create(
            Long meetingId,
            Long userId,
            SchedulePollRequest.Create request
    ) {

       Meeting meeting = getActiveMeetingForHost(meetingId, userId);

       if(schedulePollRepository.existsByMeetingId(meetingId)){
           throw new SchedulePollAlreadyExistsException(meetingId);
       }

       SchedulePoll savedPoll =
               schedulePollRepository.save(SchedulePoll.create(meeting,request.deadline()));

       return SchedulePollResponse.Created.from(savedPoll);
    }

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

        if(!meetingMember.isJoined()){
            throw new MeetingMemberAlreadyLeftException(meetingId, userId);
        }

        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingIdWithCandidates(meetingId)
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

        getActiveMeetingForHost(meetingId, userId);

        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingIdForUpdate(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );

        schedulePoll.updateDeadline(request.deadline(),LocalDateTime.now());

        return SchedulePollResponse.DeadlineUpdate.from(
                schedulePoll
        );

    }

    private Meeting getActiveMeetingForHost(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        validateHost(meeting, userId);

        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meetingId);
        }

        return meeting;
    }

    private void validateHost(
            Meeting meeting,
            Long userId
    ) {
        if (!meeting.isHost(userId)) {
            throw new MeetingHostRequiredException();
        }
    }

}
