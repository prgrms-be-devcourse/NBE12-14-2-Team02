package com.prgms.backend.domain.schedule.poll.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
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
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
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
    private final MeetingRepository meetingRepository;

    private record CandidateAggregation(
            Long candidateId,
            LocalDate candidateDate,
            int totalScore,
            long responseCount,
            long nonResponseCount,
            ScheduleResultResponse.PreferenceCounts preferenceCounts
    ) {
    }

    private record ResultContext(
            SchedulePoll schedulePoll,
            List<MeetingMember> joinedMembers,
            List<ScheduleVote> votes
    ) {
    }

    @Transactional(readOnly = true)
    public ScheduleResultResponse.Detail getResults(Long meetingId, Long userId) {
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

    private List<ScheduleResultResponse.CandidateRank> createCandidateRanks(
            ResultContext context
    ) {


        Map<Long, List<ScheduleVote>> votesByCandidate =
                context.votes().stream().collect(
                        Collectors.groupingBy(
                                vote -> vote.getScheduleCandidate().getId()
                        )
                );

        long joinedMemberCount = context.joinedMembers().size();


        List<CandidateAggregation> aggregations =
                context.schedulePoll().getCandidates().stream()
                        .map(candidate -> {
                            List<ScheduleVote> candidateVotes =
                                    votesByCandidate.getOrDefault(
                                            candidate.getId(),
                                            List.of()
                                    );
                            int totalScore =
                                    candidateVotes.stream()
                                            .mapToInt(
                                                    ScheduleVote::getScore
                                            )
                                            .sum();
                            long preferCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.PREFER
                                    );
                            long availableCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.AVAILABLE
                                    );
                            long dislikeCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.DISLIKE
                                    );
                            long impossibleCount =
                                    countPreference(
                                            candidateVotes,
                                            SchedulePreference.IMPOSSIBLE
                                    );
                            long responseCount =
                                    candidateVotes.size();
                            long nonResponseCount =
                                    joinedMemberCount - responseCount;

                            ScheduleResultResponse.PreferenceCounts counts =
                                    new ScheduleResultResponse.PreferenceCounts(
                                            preferCount,
                                            availableCount,
                                            dislikeCount,
                                            impossibleCount
                                    );
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

    private List<ScheduleParticipantResponse.MemberResponses> createParticipantResponses(
            ResultContext context
    ) {

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

    private ResultContext getResultContext(Long meetingId, Long userId) {
        meetingRepository
            .findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        validateJoinedMember(meetingId, userId);
        SchedulePoll schedulePoll = getClosedSchedulePoll(meetingId);

        List<MeetingMember> joinedMembers =
                meetingMemberRepository.findAllByMeetingIdAndStatus(
                        meetingId,
                        MeetingMemberStatus.JOINED
                );


        Set<Long> joinedMemberIds =
                joinedMembers.stream()
                        .map(MeetingMember::getId)
                        .collect(Collectors.toSet());

        List<ScheduleVote> votes =
                scheduleVoteRepository
                        .findAllByScheduleCandidateSchedulePollId(schedulePoll.getId())
                        .stream()
                        .filter(vote -> joinedMemberIds.contains(vote.getMeetingMember().getId()))
                        .toList();
        return new ResultContext(schedulePoll, joinedMembers, votes);
    }

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

    private SchedulePoll getClosedSchedulePoll(Long meetingId) {
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingId(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );
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
