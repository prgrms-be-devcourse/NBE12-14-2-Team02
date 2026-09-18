package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.dto.request.ContentCandidateCreateRequest;
import com.prgms.backend.domain.content.dto.request.ContentCandidateUpdateRequest;
import com.prgms.backend.domain.content.dto.response.ContentCandidateResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.global.exception.custom.content.*;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentCandidateService {
    private final ContentCandidateRepository contentCandidateRepository;
    private final ContentPollRepository contentPollRepository;
    private final ContentVoteRepository contentVoteRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    //후보 생성
    @Transactional
    public ContentCandidateResponse create(
            Long meetingId,
            Long userId,
            ContentCandidateCreateRequest request
    ){
        MeetingMember member = requireJoinedMember(meetingId, userId);
        Long meetingMemberId = member.getId();
        ContentPoll poll = getOpenPoll(meetingId);

        if(contentCandidateRepository.existsByContentPollIdAndTitle(poll.getId(), request.title())){
            throw new DuplicateContentCandidateTitleException(request.title());
        }

        ContentCandidate saved = contentCandidateRepository.save(
                new ContentCandidate(poll,meetingMemberId, request.title(), request.description())
        );
        return ContentCandidateResponse.from(saved);
    }

    //후보 수정
    @Transactional
    public ContentCandidateResponse update(
            Long meetingId,
            Long candidateId,
            Long userId,
            ContentCandidateUpdateRequest request
    ){
        MeetingMember member = requireJoinedMember(meetingId, userId);
        Long meetingMemberId = member.getId();

        ContentPoll poll = getOpenPoll(meetingId);
        ContentCandidate candidate = getCandidateInPoll(candidateId, poll);

        // 접근 meetingMemberId가 후보생성 멤버가 아니면 예외
        if(!candidate.getCreatedByMemberId().equals(meetingMemberId)){
            throw new NotContentCandidateOwnerException();
        }
        // 후보가 투표가 되어있으면 예외
        if(contentVoteRepository.existsByContentCandidateId(candidateId)){
            throw new ContentCandidateHasVotesException(candidateId);
        }
        //수정하려는 제목이 다른 후보와 중복되면 예외
        if(contentCandidateRepository.existsByContentPollIdAndTitleAndIdNot(
                poll.getId(), request.title(), candidateId
        )){
            throw new DuplicateContentCandidateTitleException(request.title());
        }

        candidate.update(request.title(), request.description());
        return ContentCandidateResponse.from(candidate);
    }

    @Transactional
    public void delete(Long meetingId, Long candidateId, Long userId){
        MeetingMember member = requireJoinedMember(meetingId, userId);
        Long meetingMemberId = member.getId();

        ContentPoll poll = getOpenPoll(meetingId);
        ContentCandidate candidate = getCandidateInPoll(candidateId, poll);

        //멤버 ID가 후보만든 멤버가 아니면 예외
        if(!candidate.getCreatedByMemberId().equals(meetingMemberId)){
            throw new NotContentCandidateOwnerException();
        }
        //후보가 투표되어있으면 예외
        if(contentVoteRepository.existsByContentCandidateId(candidateId)){
            throw new ContentCandidateHasVotesException(candidateId);
        }
        contentCandidateRepository.delete(candidate);
    }

    //열려있는 투표판을 가져옴
    private ContentPoll getOpenPoll(Long meetingId){
        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if(poll.getStatus() == ContentPollStatus.CLOSED){
            throw new ContentPollClosedException();
        }
        return poll;
    }

    //투표판안의 후보를 가져옴
    private ContentCandidate getCandidateInPoll(Long candidateId,ContentPoll poll){
        ContentCandidate candidate = contentCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new ContentCandidateNotFoundException(candidateId));
        if(!candidate.getContentPoll().getId().equals(poll.getId())){
            throw new ContentCandidateNotFoundException(candidateId);
        }
        return candidate;
    }

    private MeetingMember requireJoinedMember(Long meetingId, Long userId) {
        MeetingMember member = meetingMemberRepository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new MeetingMemberNotFoundException(meetingId, userId));
        if (!member.isJoined()) {
            throw new MeetingAccessDeniedException(meetingId, userId);
        }
        return member;
    }
}
