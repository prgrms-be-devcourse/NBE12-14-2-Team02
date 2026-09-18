package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.poll.dto.ScheduleParticipantResponse;
import com.prgms.backend.domain.schedule.poll.dto.ScheduleResultResponse;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotClosedException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ScheduleResultService {

    private final SchedulePollRepository schedulePollRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final ScheduleVoteRepository scheduleVoteRepository;

    //후보 별 집계값을 중간에서 전달하기 위한 레코두
    private record CandidateAggregation(
            Long candidateId,
            LocalDate candidateDate,
            int totalScore,
            long responseCount,
            long nonResponseCount,
            ScheduleResultResponse.PreferenceCounts preferenceCounts
    ) {
    }

    // 두 결과 API에서 공통으로 사용하는 조회 결과를 묶는다.
    private record ResultContext(
            SchedulePoll schedulePoll,
            List<MeetingMember> joinedMembers,
            List<ScheduleVote> votes
    ) {
    }

    @Transactional(readOnly = true)
    public ScheduleResultResponse.Detail getResults(Long meetingId, Long userId) {
        // 공통 데이터를 한 번만 조회한 뒤 두 종류의 결과를 각각 조립한다.
        ResultContext context = getResultContext(meetingId, userId);

        List<ScheduleResultResponse.CandidateRank> candidateRanks =
                createCandidateRanks(context);
        List<ScheduleParticipantResponse.MemberResponses> participantResponses =
                createParticipantResponses(context);

        return new ScheduleResultResponse.Detail(
                candidateRanks,
                participantResponses
        );
    }

    // 후보마다 점수와 응답 수를 집계한 후 총점순으로 순위를 계산한다.
    private List<ScheduleResultResponse.CandidateRank> createCandidateRanks(
            ResultContext context
    ) {

        //votes를 candidateId별로 분류하고싶음 -> map사용 key : candidateId, value : List<ScheduleVote>
        //Collectors.groupBy를 통해서 id별로 vote를 구분
        Map<Long, List<ScheduleVote>> votesByCandidate =
                context.votes().stream().collect(
                        Collectors.groupingBy(
                                vote -> vote.getScheduleCandidate().getId()
                        )
                );

        //현재 모임에 참여하고있는 모임원 수
        long joinedMemberCount = context.joinedMembers().size();


        //집계 리스트
        List<CandidateAggregation> aggregations =
                context.schedulePoll().getCandidates().stream()
                        .map(candidate -> {
                            //해당 후보의 vote를 모두 candidateVotes리스트에 넣고 이후 연산들 진행.
                            List<ScheduleVote> candidateVotes =
                                    votesByCandidate.getOrDefault(
                                            candidate.getId(),
                                            List.of()
                                    );
                            //candidateVotes에서 해당 후보의 선호도 합
                            int totalScore =
                                    candidateVotes.stream()
                                            .mapToInt(
                                                    ScheduleVote::getScore
                                            )
                                            .sum();
                            //선호응답 수
                            long preferCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.PREFER
                                    );
                            //가능 응답 수
                            long availableCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.AVAILABLE
                                    );
                            //별로 응답 수
                            long dislikeCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.DISLIKE
                                    );
                            //불가능 응답 수
                            long impossibleCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.IMPOSSIBLE
                                    );
                            //응답자 수
                            long responseCount =
                                    candidateVotes.size();
                            //미 응답자 수
                            long nonResponseCount =
                                    joinedMemberCount - responseCount;

                            ScheduleResultResponse.PreferenceCounts counts =
                                    new ScheduleResultResponse.PreferenceCounts(
                                            preferCount,
                                            availableCount,
                                            dislikeCount,
                                            impossibleCount
                                    );
                            //하나의 집계로 반환
                            return new CandidateAggregation(
                                    candidate.getId(),
                                    candidate.getCandidateDate(),
                                    totalScore,
                                    responseCount,
                                    nonResponseCount,
                                    counts
                            );
                        })
                        .toList();



        //총점 순으로 정렬
        List<CandidateAggregation> sorted =
                aggregations.stream()
                        .sorted(
                                Comparator
                                        .comparingInt(
                                                CandidateAggregation::totalScore
                                        )
                                        .reversed()
                                        .thenComparing(
                                                CandidateAggregation::candidateDate
                                        )
                        )
                        .toList();

        List<ScheduleResultResponse.CandidateRank> results = new ArrayList<>();
        Integer previousScore = null;
        int currentRank = 0;

        for (int index = 0; index < sorted.size(); index++) {
            CandidateAggregation aggregation = sorted.get(index);

            if (previousScore == null
                    || aggregation.totalScore() != previousScore) {
                currentRank = index + 1;
                previousScore = aggregation.totalScore();
            }

            results.add(
                    new ScheduleResultResponse.CandidateRank(
                            aggregation.candidateId(),
                            aggregation.candidateDate(),
                            aggregation.totalScore(),
                            currentRank,
                            aggregation.responseCount(),
                            aggregation.nonResponseCount(),
                            aggregation.preferenceCounts()
                    )
            );
        }

        return results;
    }

    // 현재 참여 중인 모임원별로 모든 후보에 대한 응답을 조립한다.
    private List<ScheduleParticipantResponse.MemberResponses> createParticipantResponses(
            ResultContext context
    ) {
        // 각 참여자의 투표를 candidateId로 바로 찾을 수 있게 변환한다.
        // 같은 참여자가 같은 후보에 두 번 투표할 수 없으므로 toMap을 사용할 수 있다.
        Map<Long, Map<Long, ScheduleVote>> votesByMember =
                context.votes().stream()
                        .collect(
                                Collectors.groupingBy(
                                        vote -> vote.getMeetingMember().getId(),
                                        Collectors.toMap(
                                                vote -> vote.getScheduleCandidate().getId(),
                                                Function.identity()
                                        )
                                )
                        );

        // 모든 참여자에게 같은 후보 순서를 제공하기 위해 날짜순으로 정렬한다.
        List<ScheduleCandidate> candidates =
                context.schedulePoll().getCandidates().stream()
                        .sorted(Comparator.comparing(ScheduleCandidate::getCandidateDate))
                        .toList();

        return context.joinedMembers().stream()
                .sorted(Comparator.comparing(MeetingMember::getId))
                .map(member -> toMemberResponses(
                        member,
                        candidates,
                        votesByMember.getOrDefault(member.getId(), Map.of())
                ))
                .toList();
    }

    // 결과 조회에 공통으로 필요한 권한 검증, 마감 검증, 참여자·투표 조회를 한곳에서 수행한다.
    private ResultContext getResultContext(Long meetingId, Long userId) {
        validateJoinedMember(meetingId, userId);
        //마감된 투표를 가져온다.
        SchedulePoll schedulePoll = getClosedSchedulePoll(meetingId);

        //현재 참여중인 모임원
        List<MeetingMember> joinedMembers =
                meetingMemberRepository.findAllByMeetingIdAndStatus(
                        meetingId,
                        MeetingMemberStatus.JOINED
                );


        Set<Long> joinedMemberIds =
                joinedMembers.stream()
                        .map(MeetingMember::getId)
                        .collect(Collectors.toSet());

        //모든 투표를 가져오고, 참여중인 모임원이 한 투표가 아니라면, filter를 통해 제거한다.
        List<ScheduleVote> votes =
                scheduleVoteRepository
                        .findAllByScheduleCandidateSchedulePollId(schedulePoll.getId())
                        .stream()
                        .filter(vote -> joinedMemberIds.contains(vote.getMeetingMember().getId()))
                        .toList();
        //SchedulePoll, 참여자 수, vote를 반환
        return new ResultContext(schedulePoll, joinedMembers, votes);
    }

    // 요청한 사용자가 현재 참여 중인 모임원인지 확인한다.
    private void validateJoinedMember(Long meetingId, Long userId) {
        MeetingMember meetingMember =
                meetingMemberRepository.findByMeetingIdAndUserId(meetingId, userId)
                        .orElseThrow(
                                () -> new MeetingMemberNotFoundException(meetingId, userId)
                        );

        if (!meetingMember.isJoined()) {
            throw new MeetingMemberAlreadyLeftException(meetingId, userId);
        }
    }

    //마감된 투표를 가져온다.
    private SchedulePoll getClosedSchedulePoll(Long meetingId) {
        //투표가 없다면 예외
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingId(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );
        //투표가 마감되지 않았다면 예외
        if (schedulePoll.getStatus() != SchedulePollStatus.CLOSED) {
            throw new SchedulePollNotClosedException();
        }

        return schedulePoll;
    }

    private ScheduleParticipantResponse.MemberResponses toMemberResponses(
            MeetingMember member,
            List<ScheduleCandidate> candidates,
            Map<Long, ScheduleVote> votesByCandidate
    ) {
        List<ScheduleParticipantResponse.CandidateAnswer> answers =
                candidates.stream()
                        .map(candidate -> {
                            ScheduleVote vote = votesByCandidate.get(candidate.getId());

                            // 투표 행이 없으면 null을 내려 미응답 상태를 표현한다.
                            SchedulePreference preference =
                                    vote == null ? null : vote.getPreference();

                            return new ScheduleParticipantResponse.CandidateAnswer(
                                    candidate.getId(),
                                    candidate.getCandidateDate(),
                                    preference
                            );
                        })
                        .toList();

        return new ScheduleParticipantResponse.MemberResponses(
                member.getId(),
                member.getUser().getId(),
                member.getUser().getNickname(),
                answers
        );
    }

    private long countPreference(
            List<ScheduleVote> votes,
            SchedulePreference preference
    ) {
        return votes.stream()
                .filter(vote ->
                        vote.getPreference() == preference
                )
                .count();
    }
}
