package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentPollCreateRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollDetailResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.content.ContentPollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.content.ContentPollNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentPollService {
    private final ContentPollRepository contentPollRepository;
    private final MeetingRepository meetingRepository;
    private final ContentCandidateRepository contentCandidateRepository;
    private final ContentVoteRepository contentVoteRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    @Transactional
    public ContentPollResponse create(Long meetingId, Long userId, ContentPollCreateRequest request){
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        if (contentPollRepository.existsByMeetingId(meetingId)){
            throw new ContentPollAlreadyExistsException(meetingId);
        }

        ContentPoll saved = contentPollRepository.save(
                new ContentPoll(meetingId,request.deadline())
        );

        return ContentPollResponse.from(saved);
    }

    @Transactional
    public ContentPollDetailResponse get(Long meetingId, Long userId){
        MeetingMember member = requireJoinedMember(meetingId, userId);
        Long meetingMemberId = member.getId();

        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        //해당 poll의 후보들을 등록시간 기준으로 정렬
        List<ContentCandidate> candidates = contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(poll.getId());

        //후보 id당 선호도투표들로 매핑
        Map<Long, List<ContentVote>> votesByCandidate = contentVoteRepository
                .findByContentCandidate_ContentPoll_Id(poll.getId())
                .stream()
                .collect(Collectors.groupingBy(v -> v.getContentCandidate().getId()));

        //후보마다 점수
        List<ContentPollDetailResponse.CandidateRank> ranked = candidates.stream()
                .map(candidate -> {
                    List<ContentVote> votes = votesByCandidate.getOrDefault(candidate.getId(), List.of());
                    int totalScore = votes.stream()
                            .mapToInt(v -> v.getPreference().score())
                            .sum();
                    ContentPreference myPreference = votes.stream()
                            .filter(v -> v.getMeetingMemberId().equals(meetingMemberId))
                            .map(ContentVote::getPreference)
                            .findFirst()
                            .orElse(null);
                    return new ContentPollDetailResponse.CandidateRank(
                            candidate.getId(),
                            candidate.getCreatedByMemberId(),
                            candidate.getTitle(),
                            candidate.getDescription(),
                            totalScore,
                            myPreference
                    );
                })
                .sorted(Comparator
                        .comparingInt(ContentPollDetailResponse.CandidateRank::totalScore)
                        .reversed())
                .toList();

        return new ContentPollDetailResponse(
                poll.getId(),
                poll.getMeetingId(),
                poll.getDeadline(),
                poll.getStatus(),
                ranked
        );


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
