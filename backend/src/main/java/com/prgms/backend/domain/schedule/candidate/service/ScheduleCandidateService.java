package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScheduleCandidateService {
    private final ScheduleCandidateRepository scheduleCandidateRepository;


}
