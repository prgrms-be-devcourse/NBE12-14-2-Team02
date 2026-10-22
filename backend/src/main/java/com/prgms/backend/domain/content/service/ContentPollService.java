package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentPollRequest;
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
import java.util.Set;
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
    public ContentPollResponse.Created create(Long meetingId, Long userId, ContentPollRequest.Create request){
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

        return ContentPollResponse.Created.from(saved);
    }

    @Transactional
    public ContentPollResponse.Detail get(Long meetingId, Long userId){
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

        boolean closed = poll.getStatus() == ContentPollStatus.CLOSED;
        // 진행 중에는 점수와 순위를 숨기고, 마감 후에만 점수로 정렬한다.
        List<ContentPollResponse.CandidateRank> ranked = candidates.stream()
                .map(candidate -> {
                    List<ContentVote> votes = votesByCandidate.getOrDefault(candidate.getId(), List.of());
                    int totalScore = closed
                            ? votes.stream().mapToInt(v -> v.getPreference().score()).sum()
                            : 0;
                    ContentPreference myPreference = votes.stream()
                            .filter(v -> v.getMeetingMemberId().equals(meetingMemberId))
                            .map(ContentVote::getPreference)
                            .findFirst()
                            .orElse(null);
                    return new ContentPollResponse.CandidateRank(
                            candidate.getId(),
                            candidate.getCreatedByMemberId(),
                            candidate.getTitle(),
                            candidate.getDescription(),
                            totalScore,
                            myPreference
                    );
                })
                .toList();
        if (closed) {
            ranked = ranked.stream()
                    .sorted(Comparator.comparingInt(ContentPollResponse.CandidateRank::totalScore).reversed())
                    .toList();
        }

        return new ContentPollResponse.Detail(
                poll.getId(),
                poll.getMeetingId(),
                poll.getDeadline(),
                poll.getStatus(),
                ranked
        );


    }
    // 콘텐츠 투표 마감기한 업데이트
    @Transactional
    public ContentPollResponse.DeadlineUpdate updateDeadline(
            Long meetingId,
            Long userId,
            ContentPollRequest.UpdateDeadline request
    ){
        // 마감 스케줄러와 같은 순서로 잠금
        Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNullForShare(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        ContentPoll poll = contentPollRepository.findByMeetingIdForUpdate(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if(poll.getStatus() == ContentPollStatus.CLOSED){
            throw new ContentPollClosedException();
        }
        poll.changeDeadLine(request.deadline());
        return ContentPollResponse.DeadlineUpdate.from(poll);
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
    public ContentPollResultsResponse.Detail getResults(
            Long meetingId,
            Long userId,
            ContentPollResultSort sort
    ){
        requireJoinedMember(meetingId, userId);
        Meeting meeting = meetingRepository.findById(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        ContentPoll poll = contentPollRepository.findByMeetingId(meetingId)
                .orElseThrow(() -> new ContentPollNotFoundException(meetingId));
        if (poll.getStatus() != ContentPollStatus.CLOSED) {
            throw new ContentPollNotClosedException("투표가 진행 중일 때는 결과를 볼 수 없습니다.");
        }

        List<ContentCandidate> candidates = contentCandidateRepository.findByContentPollIdOrderByCreatedAtAsc(poll.getId());

        //모든 표들을 가져와서 후보별로 맵핑
        List<ContentVote> allVotes = contentVoteRepository
                .findByContentCandidate_ContentPoll_Id(poll.getId());
        Map<Long,List<ContentVote>> votesByCandidate = allVotes.stream()
                .collect(Collectors.groupingBy(v -> v.getContentCandidate().getId()));

        // 탈퇴자 처리 기준: 현재 참여 중인 모임원 + 표를 낸 탈퇴자는 참여 인원으로 포함한다.
        // 표(점수/응답 수)와 참여 인원/미응답 수/인원별 결과가 항상 같은 모집단을 보도록 맞춘다.
        // (투표 전에 이미 탈퇴한 사람은 표가 없으므로 미응답으로 잡히지 않는다.)
        Set<Long> voterIds = allVotes.stream()
                .map(ContentVote::getMeetingMemberId)
                .collect(Collectors.toSet());
        List<MeetingMember> allMembers = meetingMemberRepository.findAllByMeetingId(meetingId);
        List<MeetingMember> joined = allMembers.stream()
                .filter(m -> m.isJoined() || voterIds.contains(m.getId()))
                .toList();
        int joinedCount = joined.size();

        // 후보 등록자가 탈퇴했어도 닉네임을 표시할 수 있도록 전체 모임원 기준으로 만든다.
        Map<Long, String> nicknameByMemberId = allMembers.stream()
                .collect(Collectors.toMap(
                        MeetingMember::getId,
                        m -> m.getUser().getNickname()
                ));

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

        return new ContentPollResultsResponse.Detail(
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
    public ContentPollResponse.Confirmed confirm(
            Long meetingId,
            Long userId,
            ContentPollRequest.Confirm request
    ){
        // 중복 확정 요청이 겹치지 않도록 Meeting 공유 잠금 -> 투표판 FOR UPDATE
        Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNullForShare(meetingId)
                .orElseThrow(() -> new MeetingNotFoundException(meetingId));
        if (!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        ContentPoll poll = contentPollRepository.findByMeetingIdForUpdate(meetingId)
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
        return ContentPollResponse.Confirmed.from(poll);
    }

}
