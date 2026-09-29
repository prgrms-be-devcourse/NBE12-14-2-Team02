package com.prgms.backend.domain.schedule.support;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public final class ScheduleTestFixtures {
    private ScheduleTestFixtures() {
    }

    public static User user(long id, String nickname) {
        User user = new User(nickname + "@example.com", nickname, "password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    public static Meeting meeting(long id, User host) {
        Meeting meeting = new Meeting(host, "테스트 모임", "일정 조율");
        ReflectionTestUtils.setField(meeting, "id", id);
        return meeting;
    }

    public static MeetingMember member(long id, Meeting meeting, User user) {
        MeetingMember member = new MeetingMember(meeting, user);
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    public static SchedulePoll poll(long id, Meeting meeting, LocalDateTime deadline) {
        SchedulePoll poll = SchedulePoll.create(meeting, deadline);
        ReflectionTestUtils.setField(poll, "id", id);
        return poll;
    }

    public static ScheduleCandidate candidate(long id, SchedulePoll poll, LocalDate date) {
        ScheduleCandidate candidate = poll.addCandidate(date);
        ReflectionTestUtils.setField(candidate, "id", id);
        return candidate;
    }

    public static ScheduleVote vote(ScheduleCandidate candidate, MeetingMember member,
                                    SchedulePreference preference) {
        return ScheduleVote.create(candidate, member, preference);
    }
}
