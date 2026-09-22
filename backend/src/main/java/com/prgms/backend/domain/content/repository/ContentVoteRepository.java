package com.prgms.backend.domain.content.repository;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.entity.ContentVote;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContentVoteRepository extends JpaRepository<ContentVote, Long> {
    Optional<ContentVote> findByContentCandidateIdAndMeetingMemberId(Long contentCandidateId, Long meetingMemberId);

    List<ContentVote> findByContentCandidateId(Long contentCandidateId);

    //투표가 달린 후보 확인
    boolean existsByContentCandidateId(Long contentCandidateId);

    void deleteByContentCandidateIdAndMeetingMemberId(
            Long contentCandidateId, Long meetingMemberId
    );

    List<ContentVote> findByContentCandidate_ContentPoll_Id(Long contentPollId);

    @Modifying
    @Query("""
    DELETE FROM ContentVote cv
    WHERE cv.meetingMemberId = :meetingMemberId
      AND cv.contentCandidate.id IN (
          SELECT cc.id
          FROM ContentCandidate cc
          JOIN cc.contentPoll cp
          WHERE cp.meetingId = :meetingId
            AND cp.status = :status
            AND cp.deadline > :now
      )
    """)
    int deleteOpenVotesByMeetingMember(
        @Param("meetingId") Long meetingId,
        @Param("meetingMemberId") Long meetingMemberId,
        @Param("status") ContentPollStatus status,
        @Param("now") LocalDateTime now
    );
}
