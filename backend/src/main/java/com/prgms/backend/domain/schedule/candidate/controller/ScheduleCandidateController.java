package com.prgms.backend.domain.schedule.candidate.controller;

import com.prgms.backend.domain.schedule.candidate.service.ScheduleCandidateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings/{meetingId}/schedule-poll/candidates")
@RequiredArgsConstructor
public class ScheduleCandidateController {
    private final ScheduleCandidateService scheduleCandidateService;

}
