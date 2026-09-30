package com.prgms.backend.domain.content.entity;

import com.prgms.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "content_candidates")
public class ContentCandidate extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_poll_id", nullable = false)
    private ContentPoll contentPoll;

    @Column(name = "created_by_member_id", nullable = false)
    private Long createdByMemberId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 500)
    private String description;

    public ContentCandidate(ContentPoll contentPoll,Long createdByMemberId, String title, String description){
        this.contentPoll = contentPoll;
        this.createdByMemberId = createdByMemberId;
        this.title = title;
        this.description = description;
    }

    public void update(String title, String description){
        this.title = title;
        this.description = description;
    }
}
