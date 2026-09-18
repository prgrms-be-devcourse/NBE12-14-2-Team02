package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.dto.request.ContentVoteRequest;
import com.prgms.backend.domain.content.dto.response.ContentVoteResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.global.exception.custom.content.ContentCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.content.ContentPollClosedException;
import com.prgms.backend.global.exception.custom.content.ContentPollNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentVoteService {
    private final ContentVoteRepository contentVoteRepository;
    private final ContentCandidateRepository contentCandidateRepository;
    private final ContentPollRepository contentPollRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    @Transactional
    public ContentVoteResponse upsert(
            Long meetingId,
            Long userId,
            ContentVoteRequest request
    ){
        MeetingMember member = requireJoinedMember(meetingId, userId);
        Long meetingMemberId = member.getId();

        ContentPoll poll = getOpenPoll(meetingId);
        ContentCandidate candidate = getCandidateInPoll(request.candidateId(), poll);

        //투표가 있으면 변경, 없으면 추가
        ContentVote vote = contentVoteRepository
                .findByContentCandidateIdAndMeetingMemberId(candidate.getId(), meetingMemberId)
                .map(existing -> {
                    existing.changePreference(request.preference());
                    return existing;
                })
                .orElseGet(() -> contentVoteRepository.save(
                        new ContentVote(candidate, meetingMemberId, request.preference())
                ));

        return ContentVoteResponse.from(vote);
    }

    private ContentPoll getOpenPoll(Long meetingId){
        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if(poll.getStatus() == ContentPollStatus.CLOSED){
            throw new ContentPollClosedException();
        }
        return poll;
    }

    private ContentCandidate getCandidateInPoll(Long candidateId,ContentPoll poll){
        ContentCandidate candidate = contentCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new ContentCandidateNotFoundException(candidateId));
        if(!candidate.getContentPoll().getId().equals(poll.getId())){
            throw new ContentCandidateNotFoundException(candidateId);
        }
        return candidate;
    }

    private MeetingMember requireJoinedMember(Long meetingId, Long userId){
        MeetingMember member = meetingMemberRepository
                .findByMeetingIdAndUserId(meetingId,userId)
                .orElseThrow(() -> new MeetingMemberNotFoundException(meetingId,userId));

        if(!member.isJoined()){
            throw new MeetingAccessDeniedException(meetingId,userId);
        }
        return member;
    }
}
