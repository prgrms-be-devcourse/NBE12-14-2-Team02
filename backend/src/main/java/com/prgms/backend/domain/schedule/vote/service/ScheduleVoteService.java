package com.prgms.backend.domain.schedule.vote.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteResponse;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleVoteService {
    private final ScheduleVoteRepository scheduleVoteRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final SchedulePollRepository schedulePollRepository;
    private final MeetingRepository meetingRepository;


    @Transactional
    public ScheduleVoteResponse.Saved submit(
            Long meetingId,
            Long candidateId,
            Long userId,
            ScheduleVoteRequest.Submit request
    ) {

        Meeting meeting = getMeetingWithSharedLock(meetingId);
        validateActiveMeeting(meeting);

        SchedulePoll schedulePoll = getPollWithSharedLock(meetingId);
        schedulePoll.validateOpen(LocalDateTime.now());

        MeetingMember meetingMember = getMeetingMemberWithExclusiveLock(meetingId, userId);
        validateJoinedMember(meetingMember, meetingId, userId);

        ScheduleCandidate candidate = schedulePoll.findCandidate(candidateId);

        ScheduleVote savedVote = saveOrUpdateVote(
                candidate,
                meetingMember,
                request.preference()
        );

        return ScheduleVoteResponse.Saved.from(savedVote);
    }

    private ScheduleVote saveOrUpdateVote(
            ScheduleCandidate candidate,
            MeetingMember meetingMember,
            SchedulePreference preference
    ) {
        return scheduleVoteRepository
                .findByScheduleCandidateIdAndMeetingMemberId(
                        candidate.getId(),
                        meetingMember.getId()
                )
                .map(existingVote -> {
                    existingVote.changePreference(preference);
                    return existingVote;
                })
                .orElseGet(() ->
                        scheduleVoteRepository.save(
                                ScheduleVote.create(candidate, meetingMember, preference)
                        )
                );
    }

    private Meeting getMeetingWithSharedLock(Long meetingId){
        return meetingRepository.findByIdAndDeletedAtIsNullForShare(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
    }

    private SchedulePoll getPollWithSharedLock(Long meetingId){
        return schedulePollRepository.findByMeetingIdForShare(meetingId)
                .orElseThrow(
                        () -> new SchedulePollNotFoundException(meetingId)
                );
    }

    private void validateActiveMeeting(Meeting meeting){
        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meeting.getId());
        }
    }

    private MeetingMember getMeetingMemberWithExclusiveLock(Long meetingId, Long userId){
        return meetingMemberRepository
                        .findByMeetingIdAndUserIdForUpdate(meetingId, userId)
                        .orElseThrow(
                                () -> new MeetingMemberNotFoundException(meetingId, userId)
                        );
    }

    private void validateJoinedMember(MeetingMember meetingMember, Long meetingId, Long userId){
        if (!meetingMember.isJoined()) {
            throw new MeetingMemberAlreadyLeftException(meetingId, userId);
        }
    }

}
