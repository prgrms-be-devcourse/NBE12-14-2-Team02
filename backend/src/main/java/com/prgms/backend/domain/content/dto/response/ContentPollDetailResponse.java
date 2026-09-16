package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;

import java.time.LocalDateTime;
import java.util.List;

public record ContentPollDetailResponse(
        Long id,
        Long meetingId,
        LocalDateTime deadline,
        ContentPollStatus status,
        List<CandidateRank> candidates
) {
    public record CandidateRank(
            Long candidateId,
            Long createdByMemberId,
            String title,
            String description,
            int totalScore,
            ContentPreference myPreference
    ){}
}
