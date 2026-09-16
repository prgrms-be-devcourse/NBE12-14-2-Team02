package com.prgms.backend.domain.content.repository;

import com.prgms.backend.domain.content.entity.ContentCandidate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentCandidateRepository extends JpaRepository<ContentCandidate, Long> {

    //동점일 경우는 먼저 등록한 후보순으로 리스트
    List<ContentCandidate> findByContentPollIdOrderByCreatedAtAsc(Long contentPollId);

    //같은 콘텐츠폴에서는 동일 제목 방지
    boolean existsByContentPollIdAndTitle(Long contentPollId, String title);

    //수정 시 제목 중복 검사 용
    boolean existsByContentPollIdAndTitleAndIdNot(Long contentPollId, String title, Long id);
}
