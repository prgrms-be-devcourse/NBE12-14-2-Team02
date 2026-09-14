package com.prgms.backend.domain.schedule.service;

import com.prgms.backend.domain.schedule.dto.SchedulePollCreateRequest;
import com.prgms.backend.domain.schedule.dto.SchedulePollResponse;
import org.springframework.stereotype.Service;

@Service
public class SchedulePollService {

    public SchedulePollResponse create(
            Long meetingId,
            SchedulePollCreateRequest request
    ) {
        throw new UnsupportedOperationException("아직 구현되지 않았습니다.");
    }
}