package com.prgms.backend.domain.meeting.entity;


import com.prgms.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "meeting_invitations")
public class MeetingInvitation extends BaseCreatedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meeting_Id", nullable = false)
    private Meeting meeting;

    // 모임 별 초대 코드는 유일해야 하므로 unique
    // 초대 url은 프론트에서 inviteCode를 사용해 생성하므로 엔티티에 선언X
    @Column(name = "invite_code", nullable = false, unique = true)
    private String inviteCode;

    // 초대 코드 만료 시점
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public MeetingInvitation(
            Meeting meeting,
            String inviteCode,
            LocalDateTime expiresAt
    ) {
        this.meeting = meeting;
        this.inviteCode = inviteCode;
        this.expiresAt = expiresAt;
    }
}
