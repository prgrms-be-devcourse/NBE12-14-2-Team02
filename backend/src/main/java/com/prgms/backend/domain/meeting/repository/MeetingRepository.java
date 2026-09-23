package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.Meeting;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByHostId(Long hostId);

    Optional<Meeting> findByIdAndDeletedAtIsNull(Long meetingId);

    // Meeting 행 잠그기
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
    SELECT m
    FROM Meeting m
    WHERE m.id = :meetingId
      AND m.deletedAt IS NULL
    """)
    Optional<Meeting> findByIdAndDeletedAtIsNullForUpdate(
        @Param("meetingId") Long meetingId
    );
}