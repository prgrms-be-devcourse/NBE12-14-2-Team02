package com.prgms.backend.domain.schedule.poll.dto;


import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ScheduleResultResponse{
    // 결과 화면에서 필요한 후보별 순위와 참여자별 응답을 한 번에 반환한다.
    public record Detail(
            List<CandidateRank> candidateRanks,
            List<ScheduleParticipantResponse.MemberResponses> participantResponses
    ) {
    }

    //후보 rank를 조회하기 위한 dto
    public record CandidateRank(
            Long candidateId,
            LocalDate candidateDate,
            int totalScore,
            int rank,
            long responseCount,
            long nonResponseCount,
            PreferenceCounts preferenceCounts
    ){
    }

    //선호도별 응답자수를 반환하기 위한 dto(dto 안의 dto)
    public record PreferenceCounts(
            long prefer,
            long available,
            long dislike,
            long impossible
    ){
    }

}
