package com.prgms.backend.domain.meeting.entity;

import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
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

    private String name;

    private String description;

    public Meeting(User host, String name, String description){
        this.host = host;
        this.name = name;
        this.description = description;
    }

    // 이 모임에서 특정 모임원이 모임장인지 검사
    public boolean isHost(Long userId){
        return this.host.getId().equals(userId);
    }
}
