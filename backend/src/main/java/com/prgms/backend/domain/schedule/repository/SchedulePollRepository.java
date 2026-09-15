package com.prgms.backend.domain.schedule.repository;

import com.prgms.backend.domain.schedule.entity.SchedulePoll;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SchedulePollRepository extends JpaRepository<SchedulePoll, Long> {
}
