package com.prgms.backend.domain.content.repository;

import com.prgms.backend.domain.content.entity.ContentPoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ContentPollRepository extends JpaRepository<ContentPoll,Long> {
    Optional<ContentPoll> findByMeetingId(Long meetingId);

    // 이 모임에 투표판이 존제는지에 대한 함수
    boolean existsByMeetingId(Long meetingId);
}
