package com.prgms.backend.domain.meeting.controller;

import com.prgms.backend.domain.meeting.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/meetings")
public class MeetingController {
    private final MeetingService meetingService;
}
