package com.prgms.backend.domain.content.entity;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "content_polls",
        //한 모임에 ContentPoll은 하나만 들어올 수 있게
        uniqueConstraints = @UniqueConstraint(name = "uk_content_polls_meeting",columnNames = "meeting_id")
)
public class ContentPoll extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "meeting_id", nullable = false)
    private Long meetingId;

    private LocalDateTime deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ContentPollStatus status = ContentPollStatus.OPEN;

    public ContentPoll(Long meetingId, LocalDateTime deadLine){
        this.meetingId = meetingId;
        this.deadline = deadLine;
        this.status = ContentPollStatus.OPEN;
    }

    public void changeDeadLine(LocalDateTime deadLine){
        this.deadline = deadLine;
    }

    public void close(){
        this.status = ContentPollStatus.CLOSED;
    }
}
