package com.prgms.backend.domain.schedule.poll.repository;

import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {
    boolean existsByMeetingId(Long meetingId);
    Optional<SchedulePoll> findByMeetingId(Long meetingId);

    List<SchedulePoll> findByStatusAndDeadlineLessThanEqual(SchedulePollStatus schedulePollStatus, LocalDateTime now);
}
