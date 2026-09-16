package com.prgms.backend.domain.schedule.poll.repository;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {
    boolean existsByMeetingId(Long meetingId);
    Optional<SchedulePoll> findByMeetingId(Long meetingId);
}
