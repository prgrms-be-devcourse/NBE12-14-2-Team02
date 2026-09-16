package com.prgms.backend.domain.meeting.entity;

import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "meeting_members",
    uniqueConstraints = {
        // unique 제약조건 - 한 모임에 같은 user가 중복 가입하는 것 방지
        @UniqueConstraint(
            name = "uk_meeting_member_meeting_user",
            columnNames = {"meeting_id", "user_id"}
        )
    }
)
public class MeetingMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meeting_id", nullable = false)
    private Meeting meeting;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingMemberStatus status;

    private LocalDateTime joinedAt;

    private LocalDateTime leftAt;

    public MeetingMember(Meeting meeting, User user){
        this.meeting = meeting;
        this.user = user;
        this.status = MeetingMemberStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
    }

    // 모임 탈퇴
    public void leave(){
        this.status = MeetingMemberStatus.LEFT;
        this.leftAt = LocalDateTime.now();
    }

    // 모임 재가입
    public void rejoin(){
        this.status = MeetingMemberStatus.JOINED;
        this.joinedAt = LocalDateTime.now();
        this.leftAt = null;
    }

    // 모임에 가입되어 있는지 검사
    public boolean isJoined(){
        return this.status == MeetingMemberStatus.JOINED;
    }
}
