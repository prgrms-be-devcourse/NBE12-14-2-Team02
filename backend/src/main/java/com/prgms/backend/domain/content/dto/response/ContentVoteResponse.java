package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.entity.ContentVote;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentVoteResponse {

    public record Saved(
            Long voteId,
            Long candidateId,
            Long meetingMemberId,
            ContentPreference preference,
            LocalDateTime createdAt,
            LocalDateTime updateAd
    ) {
        public static Saved from(ContentVote contentVote) {
            return new Saved(
                    contentVote.getId(),
                    contentVote.getContentCandidate().getId(),
                    contentVote.getMeetingMemberId(),
                    contentVote.getPreference(),
                    contentVote.getCreatedAt(),
                    contentVote.getUpdatedAt()
            );
        }
    }
}
