package com.prgms.backend.domain.settlement.repository;

import com.prgms.backend.domain.settlement.entity.SettlementAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, Long> {
    List<SettlementAccount> findByMeetingId(long meetingId);
    Optional<SettlementAccount> findByMeetingIdAndMemberId(long meetingId, long memberId);
}
