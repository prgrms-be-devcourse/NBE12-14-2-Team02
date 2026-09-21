package com.prgms.backend.domain.meeting.repository;

import com.prgms.backend.domain.meeting.entity.Meeting;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeetingRepository extends JpaRepository<Meeting, Long> {

    List<Meeting> findByHostId(Long hostId);

    Optional<Meeting> findByIdAndDeletedAtIsNull(Long meetingId);
}