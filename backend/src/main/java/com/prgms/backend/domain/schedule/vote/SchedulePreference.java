package com.prgms.backend.domain.schedule.vote;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SchedulePreference {
    PREFER(3),
    AVAILABLE(2),
    DISLIKE(1),
    IMPOSSIBLE(0);

    private final int score;
}
