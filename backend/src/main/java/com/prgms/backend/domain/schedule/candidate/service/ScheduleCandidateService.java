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
import org.springframework.transaction.annotation.Isolation;
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


    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleCandidateResponse.Summary create(Long meetingId, Long userId, ScheduleCandidateRequest.Create request) {

        validateHostAndActiveMeeting(meetingId, userId);

        SchedulePoll schedulePoll = getLockedOpenPoll(meetingId);

        LocalDate candidateDate = request.candidateDate();

        if (schedulePoll.hasDuplicateDate(candidateDate)) {
            throw new DuplicateScheduleCandidateException(candidateDate);
        }

        ScheduleCandidate candidate =
                schedulePoll.addCandidate(candidateDate);

        ScheduleCandidate savedCandidate =
                scheduleCandidateRepository.save(candidate);

        return ScheduleCandidateResponse.Summary.from(savedCandidate);

    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ScheduleCandidateResponse.Summary update(
            Long meetingId,
            Long candidateId,
            Long userId,
            ScheduleCandidateRequest.Update request
    ) {
        validateHostAndActiveMeeting(meetingId, userId);

        SchedulePoll schedulePoll =
                getLockedOpenPoll(meetingId);

        ScheduleCandidate candidate =
                schedulePoll.findCandidate(candidateId);

        validateCandidateHasNoVotes(candidateId);

        LocalDate candidateDate = request.candidateDate();

        if (schedulePoll.hasDuplicateDate(
                candidateId,
                candidateDate
        )) {
            throw new DuplicateScheduleCandidateException(candidateDate);
        }

        candidate.updateCandidateDate(candidateDate);

        return ScheduleCandidateResponse.Summary.from(candidate);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void delete(
            Long meetingId,
            Long candidateId,
            Long userId
    ) {
        validateHostAndActiveMeeting(meetingId, userId);

        SchedulePoll schedulePoll =
                getLockedOpenPoll(meetingId);

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


    private void validateHostAndActiveMeeting(
            Long meetingId,
            Long userId
    ) {
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(
                        () -> new MeetingNotFoundException(meetingId)
                );

        if (!meeting.isHost(userId)) {
            throw new MeetingHostRequiredException();
        }

        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meetingId);
        }
    }

    private SchedulePoll getLockedOpenPoll(Long meetingId) {
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingIdForUpdate(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );

        schedulePoll.validateOpen(LocalDateTime.now());

        return schedulePoll;
    }


}
