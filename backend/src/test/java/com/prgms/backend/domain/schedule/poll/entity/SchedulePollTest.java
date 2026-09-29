package com.prgms.backend.domain.schedule.poll.entity;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateLimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SchedulePollTest {

    private SchedulePoll poll;

    @BeforeEach
    void setUp() {
        User host = new User("host@example.com", "모임장", "password");
        Meeting meeting = new Meeting(host, "테스트 모임", "일정 조율 테스트");
        poll = SchedulePoll.create(meeting, LocalDateTime.of(2030, 1, 31, 18, 0));
    }

    @Test
    @DisplayName("일정 후보는 10개까지 등록할 수 있다")
    void addCandidatesUpToLimit() {
        LocalDate firstDate = LocalDate.of(2030, 2, 1);

        for (int i = 0; i < 10; i++) {
            poll.addCandidate(firstDate.plusDays(i));
        }

        assertEquals(10, poll.getCandidates().size());
        assertEquals(firstDate, poll.getCandidates().getFirst().getCandidateDate());
        assertEquals(firstDate.plusDays(9), poll.getCandidates().getLast().getCandidateDate());
    }

    @Test
    @DisplayName("11번째 일정 후보는 거부하고 기존 10개를 유지한다")
    void rejectCandidateBeyondLimit() {
        LocalDate firstDate = LocalDate.of(2030, 2, 1);

        for (int i = 0; i < 10; i++) {
            poll.addCandidate(firstDate.plusDays(i));
        }

        assertThrows(ScheduleCandidateLimitExceededException.class,
                () -> poll.addCandidate(firstDate.plusDays(10)));
        assertEquals(10, poll.getCandidates().size());
    }
}
