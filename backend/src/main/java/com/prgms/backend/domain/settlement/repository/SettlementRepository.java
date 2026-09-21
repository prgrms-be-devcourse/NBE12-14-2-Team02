package com.prgms.backend.domain.settlement.repository;

import com.prgms.backend.domain.settlement.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {
    Optional<Settlement> findByMeetingId(long meetingId);
}
