package com.prgms.backend.domain.content.entity;

import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor
@Table(
        name = "content_votes",
        // 한 멤버가 한 후보에게 여러 표를 못 붙이게
        uniqueConstraints = @UniqueConstraint(
                name = "uk_content_votes_candidate_member",
                columnNames = {"content_candidate_id","meeting_member_id"}
        )
)
public class ContentVote extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_candidate_id", nullable = false)
    private ContentCandidate contentCandidate;

    @Column(name = "meeting_member_id", nullable = false)
    private Long meetingMemberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContentPreference preference;

    public ContentVote(ContentCandidate contentCandidate, Long meetingMemberId, ContentPreference preference) {
        this.contentCandidate = contentCandidate;
        this.meetingMemberId = meetingMemberId;
        this.preference = preference;
    }

    public void changePreference(ContentPreference preference) {
        this.preference = preference;
    }
}
