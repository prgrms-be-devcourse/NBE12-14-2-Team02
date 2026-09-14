package com.prgms.backend.domain.notification.entity;

import com.prgms.backend.domain.notification.ENUM.NotificationType;
import com.prgms.backend.global.entity.BaseCreatedEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification extends BaseCreatedEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name ="receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "meeting_id")
    private Long meetingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String content;

    @Column(name = "redirect_url")
    private String redirectUrl;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public Notification(
            Long receiverId,
            Long meetingId,
            NotificationType type,
            String title,
            String content,
            String redirectUrl,
            boolean isRead
    ) {
        this.receiverId = receiverId;
        this.meetingId = meetingId;
        this.type = type;
        this.title = title;
        this.content = content;
        this.redirectUrl = redirectUrl;
        this.isRead = false;
    }

    public void markRead(){
        this.isRead = true;
    }
}
