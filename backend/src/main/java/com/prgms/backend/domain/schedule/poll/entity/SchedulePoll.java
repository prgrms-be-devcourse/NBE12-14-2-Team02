package com.prgms.backend.domain.schedule.poll.entity;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateLimitExceededException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollClosedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Getter
@Entity
@Table(name = "schedule_polls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulePoll {

    private final static int MAX_CANDIDATE_COUNT = 10;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //OneToOne : 1모임 1일정투표.
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "meeting_id",
            nullable = false,
            unique = true
    )
    private Meeting meeting;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SchedulePollStatus status;

    @OneToMany(
            mappedBy = "schedulePoll",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<ScheduleCandidate> candidates = new ArrayList<>();

    private SchedulePoll(
            Meeting meeting,
            LocalDateTime deadline
    ){
        this.meeting = meeting;
        this.deadline = deadline;
        this.status = SchedulePollStatus.OPEN;
    }
    public static SchedulePoll create(
            Meeting meeting,
            LocalDateTime deadline
    ){
        return new SchedulePoll(meeting, deadline);
    }

     public ScheduleCandidate addCandidate(LocalDate candidateDate) {
        if(candidates.size() >= MAX_CANDIDATE_COUNT){
            throw new ScheduleCandidateLimitExceededException(MAX_CANDIDATE_COUNT);
        }

        ScheduleCandidate candidate =
                ScheduleCandidate.create(this, candidateDate);

        candidates.add(candidate);
        return candidate;
    }

    public void updateDeadline(
            LocalDateTime newDeadline,
            LocalDateTime now
    ){
        validateOpen(now);
        this.deadline = newDeadline;
    }

    public void validateOpen(LocalDateTime now) {
        if (this.status == SchedulePollStatus.CLOSED ||
                !now.isBefore(deadline)) {
            throw new SchedulePollClosedException();
        }
    }
    public ScheduleCandidate findCandidate(Long candidateId){
        return candidates.stream()
                .filter(candidate ->
                        candidate.getId().equals(candidateId))
                .findFirst()
                .orElseThrow(
                        () -> new ScheduleCandidateNotFoundException(candidateId)
                );

    }

    public boolean hasDuplicateDate(
            Long excludedCandidateId,
            LocalDate candidateDate
    ) {
        return candidates.stream()
                .anyMatch(candidate ->
                        !candidate.getId().equals(excludedCandidateId)
                                && candidate.getCandidateDate()
                                .equals(candidateDate)
                );
    }

    public boolean hasDuplicateDate(LocalDate candidateDate) {
        return candidates.stream()
                .anyMatch(candidate ->
                        candidate.getCandidateDate()
                                .equals(candidateDate)
                );
    }


    public void removeCandidate(ScheduleCandidate candidate) {
        candidates.remove(candidate);
    }

    public void close() {
        this.status = SchedulePollStatus.CLOSED;
    }

}
