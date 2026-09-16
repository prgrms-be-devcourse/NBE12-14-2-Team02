package com.prgms.backend.domain.schedule.poll.dto;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

//하나의 생성, 조회, 수정용 DTO를 각각 만들기보다는 하나의 클래스가 DTO를 제공하는 식으로 구현
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchedulePollResponse {

    //SchedulePoll 생성 결과 반환용 DTO
    public record Created(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            SchedulePollStatus status
    ) {
        public static Created from(
                SchedulePoll schedulePoll
        ) {
            return new Created(
                    schedulePoll.getId(),
                    schedulePoll.getMeeting().getId(),
                    schedulePoll.getDeadline(),
                    schedulePoll.getStatus()
            );
        }
    }

    //조회용 DTO
    public record Detail(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            SchedulePollStatus status,
            List<ScheduleCandidateResponse.Summary> candidates
    ) {
        public static Detail from(
                SchedulePoll schedulePoll
        ) {
            List<ScheduleCandidateResponse.Summary> candidates =
                    schedulePoll.getCandidates().stream()
                            .map(ScheduleCandidateResponse.Summary::from)
                            .toList();

            return new Detail(
                    schedulePoll.getId(),
                    schedulePoll.getMeeting().getId(),
                    schedulePoll.getDeadline(),
                    schedulePoll.getStatus(),
                    candidates
            );
        }
    }
    //마감시간 수정용 DTO
    public record DeadlineUpdate(
            Long id,
            LocalDateTime deadline,
            SchedulePollStatus status
    ){
        public static DeadlineUpdate from(SchedulePoll schedulePoll){
            return new DeadlineUpdate(
                    schedulePoll.getId(),
                    schedulePoll.getDeadline(),
                    schedulePoll.getStatus()
            );
        }
    }

}

