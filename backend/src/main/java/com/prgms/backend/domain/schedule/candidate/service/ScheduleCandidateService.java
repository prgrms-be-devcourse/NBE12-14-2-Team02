package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
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


    //일정 후보 등록하고 SchedulePoll에다가 add하면, db반영.
    @Transactional
    public ScheduleCandidateResponse.Summary create(Long meetingId, Long userId, ScheduleCandidateRequest.Create request) {

        SchedulePoll schedulePoll = schedulePollRepository.findByMeetingIdWithMeetingForUpdate(meetingId)
                .orElseThrow(
                        () -> new SchedulePollNotFoundException(meetingId)
                );

        if (!schedulePoll.getMeeting().isHost(userId)) {
            throw new MeetingHostRequiredException();
        }

        schedulePoll.validateOpen(LocalDateTime.now());

        LocalDate candidateDate = request.candidateDate();

        if(schedulePoll.hasDuplicateDate(
                candidateDate
        )){
            throw new DuplicateScheduleCandidateException(
                    candidateDate
            );
        }

        ScheduleCandidate candidate = schedulePoll.addCandidate(candidateDate);

        ScheduleCandidate savedCandidate =
                scheduleCandidateRepository.save(candidate);

        return ScheduleCandidateResponse.Summary.from(savedCandidate);



    }

    @Transactional
    public ScheduleCandidateResponse.Summary update(Long meetingId, Long candidateId, Long userId, ScheduleCandidateRequest.Update request) {
        SchedulePoll schedulePoll = getEditableSchedulePoll(meetingId, userId);

        ScheduleCandidate candidate = schedulePoll.findCandidate(candidateId);

        validateCandidateHasNoVotes(candidateId);

        LocalDate candidateDate = request.candidateDate();

        if(schedulePoll.hasDuplicateDate(
                candidateId,
                candidateDate
        )){
            throw new DuplicateScheduleCandidateException(
                    candidateDate
            );
        }

        candidate.updateCandidateDate(candidateDate);

        return ScheduleCandidateResponse.Summary.from(candidate);

    }

    @Transactional
    public void delete(Long meetingId, Long candidateId, Long userId) {

        SchedulePoll schedulePoll = getEditableSchedulePoll(meetingId, userId);

        ScheduleCandidate candidate = schedulePoll.findCandidate(candidateId);

        validateCandidateHasNoVotes(candidateId);

        schedulePoll.removeCandidate(candidate);
    }

    //공통 로직 수행하는 부분 메서드 분리 schedulePoll이 현재 유효한지 + meeting이 있는 지까지 검증 + 현재 사용자가 host인지 검증
    private SchedulePoll getEditableSchedulePoll(
            Long meetingId,
            Long userId
    ) {
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingIdWithMeetingAndCandidates(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(
                                        meetingId
                                )
                        );

        if (!schedulePoll.getMeeting().isHost(userId)) {
            throw new MeetingHostRequiredException();
        }

        schedulePoll.validateOpen(LocalDateTime.now());

        return schedulePoll;
    }


    private void validateCandidateHasNoVotes(Long candidateId) {
        if (scheduleVoteRepository
                .existsByScheduleCandidateId(candidateId)) {
            throw new ScheduleCandidateHasVotesException(
                    candidateId
            );
        }
    }
}
