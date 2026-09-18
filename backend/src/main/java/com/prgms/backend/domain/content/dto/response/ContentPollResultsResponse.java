package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;

import java.time.LocalDateTime;
import java.util.List;

public record ContentPollResultsResponse(
        Long id,
        Long meetingId,
        LocalDateTime deadline,
        ContentPollStatus status,
        Long confirmedCandidateId,
        int joinedCount,
        List<CandidateResult> candidates,
        List<MemberResult> members
) {
    public record CandidateResult(
            Long candidateId,
            Long createdByMemberId,
            String createdByNickname,
            String title,
            String description,
            int totalScore,
            int preferCount,
            int availableCount,
            int dislikeCount,
            int responseCount,
            int noResponseCount
    ){}

    public record MemberResult(
            Long meetingMemberId,
            Long userId,
            String nickname,
            boolean host,
            List<ContentPreference> preferences,
            int responseCount
    ){}
}
