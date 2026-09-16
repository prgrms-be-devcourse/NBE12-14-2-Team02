package com.prgms.backend.domain.schedule.candidate.entity;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

//schedule_poll하나에 같은 일정이 여러개 있을 수 없게 unique제약조건 추가.
@Entity
@Getter
@Table(
        name = "schedule_candidates",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_candidate_poll_date",
                        columnNames = {
                                "schedule_poll_id",
                                "candidate_date"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleCandidate extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "schedule_poll_id", nullable = false)
    private SchedulePoll schedulePoll;

    @Column(name = "candidate_date", nullable = false)
    private LocalDateTime candidateDate;


    private ScheduleCandidate(
            SchedulePoll schedulePoll,
            LocalDateTime candidateDate
    ) {
        this.schedulePoll = schedulePoll;
        this.candidateDate = candidateDate;
    }
    //ScheduleCandidate 정적 팩토리 메서드
    public static ScheduleCandidate create(
            SchedulePoll schedulePoll,
            LocalDateTime candidateDate
    ) {
        return new ScheduleCandidate(
                schedulePoll,
                candidateDate
        );
    }
}
