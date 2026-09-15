package com.prgms.backend.domain.schedule.dto;

import com.prgms.backend.domain.schedule.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.entity.SchedulePollStatus;

import java.time.LocalDateTime;
import java.util.List;

//하나의 생성, 조회, 수정용 DTO를 각각 만들기보다는 하나의 클래스가 DTO를 제공하는 식으로 구현
public class SchedulePollResponse {

    //생성, 조회용 DTO
    public record Detail(
            Long id,
            Long meetingId,
            LocalDateTime deadline,
            SchedulePollStatus status,
            List<ScheduleCandidateResponse> candidates
    ) {
        //DTO를 반환하는 팩토리 메서드
        //여기서 candidates만들 때, 후보목록을 함께 가져오는데 candidate는 LAZY로딩이라, 이때 DB조회가 발생함
        public static Detail from(SchedulePoll schedulePoll) {
            List<ScheduleCandidateResponse> candidates =
                    schedulePoll.getCandidates().stream()
                            .map(ScheduleCandidateResponse::from)
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
    //마감시간 수정 용 DTO
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

