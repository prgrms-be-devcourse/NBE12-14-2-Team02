package com.prgms.backend.domain.content.dto.response;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.entity.ContentPoll;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ContentPollResponse {

    public record Created(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            ContentPollStatus status,
            Long confirmedCandidateId
    ) {
        public static Created from(ContentPoll contentPoll) {
            return new Created(
                    contentPoll.getId(),
                    contentPoll.getMeetingId(),
                    contentPoll.getDeadline(),
                    contentPoll.getStatus(),
                    contentPoll.getConfirmedCandidateId()
            );
        }
    }

    public record Detail(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            ContentPollStatus status,
            List<CandidateRank> candidates
    ) {
    }

    public record CandidateRank(
            Long candidateId,
            Long createdByMemberId,
            String title,
            String description,
            int totalScore,
            ContentPreference myPreference
    ) {
    }

    public record DeadlineUpdate(
            Long id,
            LocalDateTime deadline,
            ContentPollStatus status,
            Long confirmedCandidateId
    ) {
        public static DeadlineUpdate from(ContentPoll contentPoll) {
            return new DeadlineUpdate(
                    contentPoll.getId(),
                    contentPoll.getDeadline(),
                    contentPoll.getStatus(),
                    contentPoll.getConfirmedCandidateId()
            );
        }
    }

    public record Confirmed(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            ContentPollStatus status,
            Long confirmedCandidateId
    ) {
        public static Confirmed from(ContentPoll contentPoll) {
            return new Confirmed(
                    contentPoll.getId(),
                    contentPoll.getMeetingId(),
                    contentPoll.getDeadline(),
                    contentPoll.getStatus(),
                    contentPoll.getConfirmedCandidateId()
            );
        }
    }
}
