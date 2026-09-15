package com.prgms.backend.domain.content.repository;

import com.prgms.backend.domain.content.entity.ContentVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContentVoteRepository extends JpaRepository<ContentVote, Long> {
    Optional<ContentVote> findByContentCandidateIdAndMeetingMemberId(Long contentCandidateId, Long meetingMemberId);

    List<ContentVote> findByContentCandidateId(Long contentCandidateId);

    //투표가 달린 후보 확인
    boolean existsByContentCandidateId(Long contentCandidateId);

    void deleteByContentCandidateIdAndMeetingMemberId(
            Long contentCandidateId, Long meetingMemberId
    );
}
