package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentPollConfirmRequest;
import com.prgms.backend.domain.content.dto.request.ContentPollCreateRequest;
import com.prgms.backend.domain.content.dto.request.ContentPollDeadlineUpdateRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollDetailResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResponse;
import com.prgms.backend.domain.content.dto.response.ContentPollResultsResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.content.*;
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
    // 콘텐츠 투표 마감기한 업데이트
    @Transactional
    public ContentPollResponse updateDeadline(
            Long meetingId,
            Long userId,
            ContentPollDeadlineUpdateRequest request
    ){
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if(poll.getStatus() == ContentPollStatus.CLOSED){
            throw new ContentPollClosedException();
        }
        poll.changeDeadLine(request.deadline());
        return ContentPollResponse.from(poll);
    }
    //해당 미팅멤버 반환하는 함수
    private MeetingMember requireJoinedMember(Long meetingId, Long userId){
        MeetingMember member = meetingMemberRepository
                .findByMeetingIdAndUserId(meetingId,userId)
                .orElseThrow(() -> new MeetingMemberNotFoundException(meetingId,userId));

        if(!member.isJoined()){
            throw new MeetingAccessDeniedException(meetingId,userId);
        }
        return member;
    }

    //콘텐츠 투표 결과 조회
    @Transactional(readOnly = true)
    public ContentPollResultsResponse getResults(
            Long meetingId,
            Long userId,
            ContentPollResultSort sort
    ){
        requireJoinedMember(meetingId, userId);
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));

        //JOIN인지 확인 후 집계
        List<MeetingMember> joined = meetingMemberRepository
                .findAllByMeetingIdAndStatus(meetingId, MeetingMemberStatus.JOINED);
        int joinedCount = joined.size();

        Map<Long, String> nicknameByMemberId = joined.stream()
                .collect(Collectors.toMap(
                        MeetingMember::getId,
                        m -> m.getUser().getNickname()
                ));

        List<ContentCandidate> candidates = contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(poll.getId());

        //모든 표들을 가져와서 후보별로 맵핑
        Map<Long,List<ContentVote>> votesByCandidate = contentVoteRepository
                .findByContentCandidate_ContentPoll_Id(poll.getId())
                .stream()
                .collect(Collectors.groupingBy(v -> v.getContentCandidate().getId()));

        //후보마다 투표결과 집계 정렬기준 sort에 따라 다르게 정렬
        List<ContentPollResultsResponse.CandidateResult> candidateResults = candidates.stream()
                .map(candidate -> {
                    List<ContentVote> votes = votesByCandidate.getOrDefault(candidate.getId(), List.of());
                    int preferCount = (int) votes.stream()
                            .filter(v -> v.getPreference() == ContentPreference.PREFER)
                            .count();
                    int availableCount = (int) votes.stream()
                            .filter(v -> v.getPreference() == ContentPreference.AVAILABLE)
                            .count();
                    int dislikeCount = (int) votes.stream()
                            .filter(v -> v.getPreference() == ContentPreference.DISLIKE)
                            .count();
                    int responseCount = votes.size();
                    int totalScore = votes.stream()
                            .mapToInt(v -> v.getPreference().score())
                            .sum();
                    return new ContentPollResultsResponse.CandidateResult(
                            candidate.getId(),
                            candidate.getCreatedByMemberId(),
                            nicknameByMemberId.get(candidate.getCreatedByMemberId()),
                            candidate.getTitle(),
                            candidate.getDescription(),
                            totalScore,
                            preferCount,
                            availableCount,
                            dislikeCount,
                            responseCount,
                            joinedCount - responseCount // 미응답 카운트
                    );
                })
                .sorted(sortComparator(sort))
                .toList();

        List<Long> orderedCandidateIds = candidateResults.stream()
                .map(ContentPollResultsResponse.CandidateResult::candidateId)
                .toList();

        //후보마다 사람별 투표결과 집계
        Map<Long,Map<Long, ContentPreference>> preferenceByMemberAndCandidate =
                votesByCandidate.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.groupingBy(
                                ContentVote::getMeetingMemberId,
                                Collectors.toMap(
                                        v -> v.getContentCandidate().getId(),
                                        ContentVote::getPreference,
                                        (a,b) -> a
                                )
                        ));

        List<ContentPollResultsResponse.MemberResult> memberResults = joined.stream()
                .map(member -> {
                    Map<Long, ContentPreference> mine =
                            preferenceByMemberAndCandidate.getOrDefault(member.getId(), Map.of());
                    List<ContentPreference> preferences = orderedCandidateIds.stream()
                            .map(mine::get)
                            .toList();
                    int responseCount = (int) preferences.stream()
                            .filter(p -> p != null)
                            .count();
                    return new ContentPollResultsResponse.MemberResult(
                            member.getId(),
                            member.getUser().getId(),
                            member.getUser().getNickname(),
                            meeting.isHost(member.getUser().getId()),
                            preferences,
                            responseCount
                    );
                })
                .toList();

        return new ContentPollResultsResponse(
                poll.getId(),
                poll.getMeetingId(),
                poll.getDeadline(),
                poll.getStatus(),
                poll.getConfirmedCandidateId(),
                joinedCount,
                candidateResults,
                memberResults
        );
    }

    //정해진 타입별로 정렬시켜주는 함수
    private Comparator<ContentPollResultsResponse.CandidateResult>  sortComparator(
            ContentPollResultSort sort
    ){
        Comparator<ContentPollResultsResponse.CandidateResult> byCreated =
                Comparator.comparing(ContentPollResultsResponse.CandidateResult::candidateId);
        if(sort == ContentPollResultSort.PARTICIPANTS){
            return Comparator
                    .comparingInt(ContentPollResultsResponse.CandidateResult::responseCount)
                    .reversed()
                    .thenComparing(Comparator
                            .comparingInt(ContentPollResultsResponse.CandidateResult::totalScore)
                            .reversed())
                    .thenComparing(byCreated);
        }

        return Comparator
                .comparingInt(ContentPollResultsResponse.CandidateResult::totalScore)
                .reversed()
                .thenComparing(byCreated);
    }

    //콘텐츠 투표결과 후보 확정 시키기
    @Transactional
    public ContentPollResponse confirm(
            Long meetingId,
            Long userId,
            ContentPollConfirmRequest request
    ){
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        if (!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if(poll.getStatus() != ContentPollStatus.CLOSED) {
            throw new ContentPollNotClosedException();
        }
        if(poll.getConfirmedCandidateId() != null){
            throw new ContentPollAlreadyConfirmedException();
        }

        ContentCandidate candidate = contentCandidateRepository.findById(request.candidateId())
                .orElseThrow(() -> new ContentCandidateNotFoundException(request.candidateId()));
        if (!candidate.getContentPoll().getId().equals(poll.getId())) {
            throw new ContentCandidateNotFoundException(request.candidateId());
        }

        poll.confirm(request.candidateId());
        return ContentPollResponse.from(poll);
    }

}
