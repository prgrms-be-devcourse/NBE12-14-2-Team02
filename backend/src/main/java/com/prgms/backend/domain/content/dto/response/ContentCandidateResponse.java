package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.entity.ContentCandidate;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentCandidateResponse {

    public record Saved(
            Long candidateId,
            Long contentPollId,
            Long createdByMemberId,
            String title,
            String description,
            LocalDateTime createdAt
    ) {
        public static Saved from(ContentCandidate candidate) {
            return new Saved(
                    candidate.getId(),
                    candidate.getContentPoll().getId(),
                    candidate.getCreatedByMemberId(),
                    candidate.getTitle(),
                    candidate.getDescription(),
                    candidate.getCreatedAt()
            );
        }
    }
}
