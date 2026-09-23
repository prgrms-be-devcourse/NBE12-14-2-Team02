package com.prgms.backend.domain.schedule.candidate.entity;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

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
    private LocalDate candidateDate;


    private ScheduleCandidate(
            SchedulePoll schedulePoll,
            LocalDate candidateDate
    ) {
        this.schedulePoll = schedulePoll;
        this.candidateDate = candidateDate;
    }

    public static ScheduleCandidate create(
            SchedulePoll schedulePoll,
            LocalDate candidateDate
    ) {
        return new ScheduleCandidate(
                schedulePoll,
                candidateDate
        );
    }

    public void updateCandidateDate(
            LocalDate candidateDate
    ){
        this.candidateDate = candidateDate;
    }
}
