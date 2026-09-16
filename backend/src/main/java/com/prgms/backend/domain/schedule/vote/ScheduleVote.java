package com.prgms.backend.domain.schedule.vote;


import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

//일정 투표는 모임원 한명이 일정 후보 하나에 한번만 가능 -> unique제약조건 추가.
@Entity
@Getter
@Table(
        name = "schedule_votes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_schedule_vote_candidate_member",
                        columnNames = {
                                "schedule_candidate_id",
                                "meeting_member_id"
                        }
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScheduleVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "schedule_candidate_id",
            nullable = false
    )
    private ScheduleCandidate scheduleCandidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "meeting_member_id",
            nullable = false
    )
    private MeetingMember meetingMember;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SchedulePreference preference;


    private ScheduleVote(
            ScheduleCandidate scheduleCandidate,
            MeetingMember meetingMember,
            SchedulePreference preference
    ) {
        this.scheduleCandidate = scheduleCandidate;
        this.meetingMember = meetingMember;
        this.preference = preference;
    }

    public static ScheduleVote create(
            ScheduleCandidate scheduleCandidate,
            MeetingMember meetingMember,
            SchedulePreference preference
    ) {
        return new ScheduleVote(
                scheduleCandidate,
                meetingMember,
                preference
        );
    }

    public void changePreference(
            SchedulePreference preference
    ) {
        this.preference = preference;
    }

    public int getScore() {
        return preference.getScore();
    }

}