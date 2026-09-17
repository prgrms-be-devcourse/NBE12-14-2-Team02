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
    public ScheduleVoteResponse.Saved submit(Long meetingId, Long candidateId, Long meetingMemberId, ScheduleVoteRequest.Submit request) {



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

        //일단은 meeting도메인 건들 수 있으니까 meetingMember를 id로만 조회하고 여기서 검증
        //todo 추후 meetingMemberRepository에서 한번에 db조회 가능하도록 변경예정
        MeetingMember meetingMember =
                meetingMemberRepository.findById(meetingMemberId)
                        .orElseThrow(
                                () -> new MeetingMemberNotFoundException(
                                        meetingMemberId
                                )
                        );

        if (!meetingMember.getMeeting().getId().equals(meetingId)
                || !meetingMember.isJoined()) {
            throw new MeetingMemberNotFoundException(
                    meetingMemberId
            );
        }


        //투표가 이미 되어있다면, 수정, 투표가 안되어있다면, row생성.
        ScheduleVote vote =
                scheduleVoteRepository
                        .findByScheduleCandidateIdAndMeetingMemberId(
                                candidateId,
                                meetingMemberId
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
