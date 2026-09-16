package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.entity.ContentVote;

import java.time.LocalDateTime;

public record ContentVoteResponse(
        Long voteId,
        Long candidateId,
        Long meetingMemberId,
        ContentPreference preference,
        LocalDateTime createdAt,
        LocalDateTime updateAd
) {
    public static ContentVoteResponse from(ContentVote contentVote) {
        return new ContentVoteResponse(
                contentVote.getId(),
                contentVote.getContentCandidate().getId(),
                contentVote.getMeetingMemberId(),
                contentVote.getPreference(),
                contentVote.getCreatedAt(),
                contentVote.getUpdatedAt()
        );
    }
}
