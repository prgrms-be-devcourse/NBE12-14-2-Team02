package com.prgms.backend.domain.schedule.vote.dto;


import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ScheduleVoteResponse {
    public record Saved(
            Long candidateId,
            Long meetingMemberId,
            SchedulePreference preference
    ){
        public static Saved from(ScheduleVote vote){
            return new Saved(
                    vote.getScheduleCandidate().getId(),
                    vote.getMeetingMember().getId(),
                    vote.getPreference()
            );
        }
    }
}
