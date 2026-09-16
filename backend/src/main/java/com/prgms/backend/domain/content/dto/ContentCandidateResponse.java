package com.prgms.backend.domain.content.dto;

import com.prgms.backend.domain.content.entity.ContentCandidate;

import java.time.LocalDateTime;

public record ContentCandidateResponse(
        Long candidateId,
        Long contentPollId,
        Long createdByMemberId,
        String title,
        String description,
        LocalDateTime createdAt
) {
    public static ContentCandidateResponse from(ContentCandidate candidate) {
        return new ContentCandidateResponse(
                candidate.getId(),
                candidate.getContentPoll().getId(),
                candidate.getCreatedByMemberId(),
                candidate.getTitle(),
                candidate.getDescription(),
                candidate.getCreatedAt()
        );
    }
}
