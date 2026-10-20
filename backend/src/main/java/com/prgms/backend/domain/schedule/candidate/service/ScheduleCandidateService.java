package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.DuplicateScheduleCandidateException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateHasVotesException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleCandidateService {
    private final ScheduleCandidateRepository scheduleCandidateRepository;
    private final SchedulePollRepository schedulePollRepository;
    private final ScheduleVoteRepository scheduleVoteRepository;
    private final MeetingRepository meetingRepository;


    @Transactional
    public ScheduleCandidateResponse.Summary createCandidate(
            Long meetingId,
            Long userId,
            ScheduleCandidateRequest.Create request
    ) {
        validateActiveMeetingForHostWithSharedLock(meetingId, userId);

        SchedulePoll schedulePoll = getOpenPollWithExclusiveLock(meetingId);

        LocalDate candidateDate = request.candidateDate();
        validateUniqueCandidateDate(schedulePoll,candidateDate);
        ScheduleCandidate candidate =
                schedulePoll.addCandidate(candidateDate);

        ScheduleCandidate savedCandidate =
                scheduleCandidateRepository.save(candidate);

        return ScheduleCandidateResponse.Summary.from(savedCandidate);
    }

    @Transactional
    public ScheduleCandidateResponse.Summary update(
            Long meetingId,
            Long candidateId,
            Long userId,
            ScheduleCandidateRequest.Update request
    ) {
        validateActiveMeetingForHostWithSharedLock(meetingId, userId);

        SchedulePoll schedulePoll =
                getOpenPollWithExclusiveLock(meetingId);
        ScheduleCandidate candidate =
                schedulePoll.findCandidate(candidateId);
        validateCandidateHasNoVotes(candidateId);

        LocalDate candidateDate = request.candidateDate();
        validateUniqueCandidateDateForUpdate(schedulePoll,candidateId,candidateDate);
        candidate.updateCandidateDate(candidateDate);

        return ScheduleCandidateResponse.Summary.from(candidate);
    }

    @Transactional
    public void delete(
            Long meetingId,
            Long candidateId,
            Long userId
    ) {
        validateActiveMeetingForHostWithSharedLock(meetingId, userId);
        SchedulePoll schedulePoll =
                getOpenPollWithExclusiveLock(meetingId);
        ScheduleCandidate candidate =
                schedulePoll.findCandidate(candidateId);
        validateCandidateHasNoVotes(candidateId);
        schedulePoll.removeCandidate(candidate);
    }

    private void validateCandidateHasNoVotes(Long candidateId) {
        if (scheduleVoteRepository
                .existsByScheduleCandidateId(candidateId)) {
            throw new ScheduleCandidateHasVotesException(
                    candidateId
            );
        }
    }

    private Meeting getMeetingWithSharedLock(
            Long meetingId
    ) {
        return meetingRepository.findByIdAndDeletedAtIsNullForShare(meetingId)
                .orElseThrow(
                        () -> new MeetingNotFoundException(meetingId)
                );
    }

    private SchedulePoll getOpenPollWithExclusiveLock(Long meetingId) {
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingIdForUpdate(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );
        schedulePoll.validateOpen(LocalDateTime.now());
        return schedulePoll;
    }

    private void validateActiveMeeting(Meeting meeting){
        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meeting.getId());
        }
    }

    private void validateHost(
            Meeting meeting,
            Long userId
    ) {
        if (!meeting.isHost(userId)) {
            throw new MeetingHostRequiredException();
        }
    }

    private void validateUniqueCandidateDate(
            SchedulePoll schedulePoll,
            LocalDate candidateDate
    ){
        if (schedulePoll.hasDuplicateDate(candidateDate)) {
            throw new DuplicateScheduleCandidateException(candidateDate);
        }
    }

    private void validateActiveMeetingForHostWithSharedLock(
            Long meetingId,
            Long userId)
    {
        Meeting meeting = getMeetingWithSharedLock(meetingId);
        validateHost(meeting, userId);
        validateActiveMeeting(meeting);
    }

    private void validateUniqueCandidateDateForUpdate(
            SchedulePoll schedulePoll,
            Long excludedCandidateId,
            LocalDate candidateDate
    ){
        if (schedulePoll.hasDuplicateDate(excludedCandidateId, candidateDate)) {
            throw new DuplicateScheduleCandidateException(candidateDate);
        }
    }
}
