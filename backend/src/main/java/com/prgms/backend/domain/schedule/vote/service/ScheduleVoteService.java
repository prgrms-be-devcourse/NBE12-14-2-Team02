package com.prgms.backend.domain.schedule.vote.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteResponse;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleVoteService {
    private final ScheduleVoteRepository scheduleVoteRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final ScheduleCandidateRepository scheduleCandidateRepository;


    @Transactional
    public ScheduleVoteResponse.Saved submit(Long meetingId, Long candidateId, Long userId, ScheduleVoteRequest.Submit request) {


        MeetingMember meetingMember =
                meetingMemberRepository
                        .findByMeetingIdAndUserId(
                                meetingId,
                                userId
                        )
                        .orElseThrow(
                                () -> new MeetingMemberNotFoundException(
                                        userId
                                )
                        );

        if (!meetingMember.isJoined()) {
            throw new MeetingMemberNotFoundException(userId);
        }


        ScheduleCandidate candidate = scheduleCandidateRepository.findByIdAndMeetingIdWithSchedulePoll(
                candidateId,
                meetingId
        ).orElseThrow(
                () -> new ScheduleCandidateNotFoundException(candidateId)
        );

        SchedulePoll schedulePoll = candidate.getSchedulePoll();

        schedulePoll.validateOpen(LocalDateTime.now());


        ScheduleVote vote =
                scheduleVoteRepository
                        .findByScheduleCandidateIdAndMeetingMemberId(
                                candidateId,
                                meetingMember.getId()
                        )
                        .map(existingVote -> {
                            existingVote.changePreference(
                                    request.preference()
                            );
                            return existingVote;
                        })
                        .orElseGet(() ->
                                scheduleVoteRepository.save(
                                        ScheduleVote.create(
                                                candidate,
                                                meetingMember,
                                                request.preference()
                                        )
                                )
                        );


        return ScheduleVoteResponse.Saved.from(vote);


    }
}
