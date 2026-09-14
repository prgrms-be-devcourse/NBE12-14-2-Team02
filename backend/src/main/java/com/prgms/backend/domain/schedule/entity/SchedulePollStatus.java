package com.prgms.backend.domain.schedule.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SchedulePollStatus {
    OPEN("일정 투표 진행 중"),
    CLOSED("일정 투표 마감");
    private final String description;
}
