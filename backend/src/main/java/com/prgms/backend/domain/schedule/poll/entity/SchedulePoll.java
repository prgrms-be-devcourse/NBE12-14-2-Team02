package com.prgms.backend.domain.schedule.poll.entity;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.global.exception.custom.SchedulePollClosedException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;



@Getter
@Entity
@Table(name = "schedule_polls")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SchedulePoll {
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

    //CascadeType.All & orphanRemoval로 부모 엔티티에서 자식 생명주기를 관리.
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
        //일정 투표 처음 생성 시 OPEN
        this.status = SchedulePollStatus.OPEN;
    }
    //일정 투표 생성 팩토리 메서드
    public static SchedulePoll create(
            Meeting meeting,
            LocalDateTime deadline
    ){
        return new SchedulePoll(meeting, deadline);
    }

    //SchedulePoll에 candidate추가하는 메서드
    public void addCandidate(LocalDateTime candidateDate){
        ScheduleCandidate candidate = ScheduleCandidate.create(this,candidateDate);
         candidates.add(candidate);
    }

    public void updateDeadline(
            LocalDateTime newDeadline,
            LocalDateTime now
    ){
        if(this.status == SchedulePollStatus.CLOSED ||
        !now.isBefore(this.deadline)){
            throw new SchedulePollClosedException();
        }
        this.deadline = newDeadline;
    }


}
