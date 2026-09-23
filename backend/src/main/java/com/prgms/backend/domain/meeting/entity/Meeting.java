package com.prgms.backend.domain.meeting.entity;

import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.global.entity.BaseTimeEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "meetings")
public class Meeting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeetingStatus status = MeetingStatus.ACTIVE;

    private LocalDateTime deletedAt;

    public Meeting(User host, String name, String description){
        this.host = host;
        this.name = name;
        this.description = description;
    }

    // 이 모임에서 특정 모임원이 모임장인지 검사
    public boolean isHost(Long userId){
        return this.host.getId().equals(userId);
    }

    // 모임 수정
    public void update(String name, String description){
        this.name = name;
        this.description = description;
    }

    // 정상적인 모임 종료
    public void complete() {
        this.status = MeetingStatus.COMPLETED;
    }

    // soft delete
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    // 진행 중인 모임인지
    public boolean isActive() {
        return this.status == MeetingStatus.ACTIVE
            && this.deletedAt == null;
    }

    // 종료된 모임인지
    public boolean isCompleted() {
        return this.status == MeetingStatus.COMPLETED
            && this.deletedAt == null;
    }

    // soft delete된 모임인지
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
