package com.prgms.backend.domain.schedule.vote.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteResponse;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleVoteService {
    private final ScheduleVoteRepository scheduleVoteRepository;
    private final SchedulePollRepository schedulePollRepository;
    private final MeetingMemberRepository meetingMemberRepository;


    @Transactional
    public ScheduleVoteResponse.Saved submit(Long meetingId, Long candidateId, Long userId, ScheduleVoteRequest.Submit request) {



        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingId(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(
                                        meetingId
                                )
                        );
        schedulePoll.validateOpen(LocalDateTime.now());

        ScheduleCandidate candidate =
                schedulePoll.findCandidate(candidateId);

        //meetingId와 jwt에서 받은 userId를 이용해서 조회.
        //todo 추후 meetingMemberRepository에서 한번에 db조회 가능하도록 변경예정
        //지금은 meetingMember를 확인한 후에, joined인지 확인하는 과정을 여기서 진행.
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


        //투표가 이미 되어있다면, 수정, 투표가 안되어있다면, row생성.
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
