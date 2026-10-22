package com.prgms.backend.domain.user;

import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingMemberService;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.UserWithdrawService;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberHasUnsettledExpenseException;
import com.prgms.backend.support.MySqlContainerSupport;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doAnswer;

@DataJpaTest(showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserWithdrawService.class, MeetingMemberService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class UserWithdrawIntegrationTest extends MySqlContainerSupport {

    @Autowired private UserWithdrawService service;
    @MockitoSpyBean private MeetingMemberService memberService;
    @MockitoSpyBean private MeetingMemberRepository members;
    @Autowired private UserRepository users;
    @Autowired private MeetingRepository meetings;
    @Autowired private ExpenseRepository expenses;
    @Autowired private EntityManager entityManager;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private Long userId;
    private Long firstMeetingId;
    private Long secondMeetingId;
    private Long secondMemberId;

    @BeforeEach
    void setUp() {
        transactions.executeWithoutResult(status -> {
            String suffix = UUID.randomUUID().toString();
            User host = users.save(new User("host-" + suffix + "@test.com", "host-" + suffix, "encoded"));
            User user = users.save(new User("user-" + suffix + "@test.com", "user-" + suffix, "encoded"));
            user.updateRefreshToken("refresh-token");
            userId = user.getId();
            Meeting first = meetings.save(new Meeting(host, "첫 모임", ""));
            Meeting second = meetings.save(new Meeting(host, "두 번째 모임", ""));
            firstMeetingId = first.getId();
            secondMeetingId = second.getId();
            members.save(new MeetingMember(first, user));
            secondMemberId = members.save(new MeetingMember(second, user)).getId();
        });

        // DB의 반환 순서에 의존하지 않고 실제 조회 결과의 처리 순서만 고정한다.
        doAnswer(invocation -> members.findAllByUserIdAndStatus(userId, MeetingMemberStatus.JOINED)
            .stream().filter(member -> member.getMeeting().getDeletedAt() == null)
            .sorted(Comparator.comparing(member -> member.getMeeting().getId())).toList())
            .when(members).findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(userId, MeetingMemberStatus.JOINED);
    }

    @Test
    @DisplayName("첫 모임 탈퇴를 DB에 반영한 뒤 두 번째 모임에서 실패하면 전체 변경을 롤백한다")
    void laterFailureRollsBackEarlierMembershipChange() {
        expenses.saveAndFlush(Expense.register(secondMeetingId, secondMemberId, "미정산 지출", 100,
            null, Map.of(secondMemberId, 100L), "EXACT", null, null));

        doAnswer(invocation -> {
            // 첫 모임의 변경이 실제 SQL로 실행됐음을 확인한 뒤 실제 업무 예외를 발생시킨다.
            entityManager.flush();
            assertEquals("LEFT", membershipStatus(firstMeetingId));
            assertNotNull(jdbc.queryForObject(
                "SELECT left_at FROM meeting_members WHERE meeting_id = ? AND user_id = ?",
                java.sql.Timestamp.class, firstMeetingId, userId));
            return invocation.callRealMethod();
        }).when(memberService).leaveMeeting(secondMeetingId, userId);

        assertThrows(MeetingMemberHasUnsettledExpenseException.class, () -> service.withdraw(userId));

        // 서비스 트랜잭션이 종료된 후 DB를 직접 조회해 1차 캐시의 영향을 배제한다.
        assertEquals("JOINED", membershipStatus(firstMeetingId));
        assertEquals("JOINED", membershipStatus(secondMeetingId));
        assertEquals(0, jdbc.queryForObject(
            "SELECT COUNT(*) FROM meeting_members WHERE user_id = ? AND left_at IS NOT NULL", Integer.class, userId));
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId));
        assertNull(jdbc.queryForObject("SELECT deleted_at FROM users WHERE id = ?", java.sql.Timestamp.class, userId));
        assertEquals("refresh-token", jdbc.queryForObject("SELECT refresh_token FROM users WHERE id = ?", String.class, userId));
    }

    @Test
    @DisplayName("탈퇴에 성공하면 모든 모임 탈퇴와 회원 상태 변경 및 토큰 삭제를 함께 커밋한다")
    void successfulWithdrawalCommitsAllChanges() {
        service.withdraw(userId);

        assertEquals("LEFT", membershipStatus(firstMeetingId));
        assertEquals("LEFT", membershipStatus(secondMeetingId));
        assertEquals(2, jdbc.queryForObject(
            "SELECT COUNT(*) FROM meeting_members WHERE user_id = ? AND left_at IS NOT NULL", Integer.class, userId));
        assertEquals("WITHDRAWN", jdbc.queryForObject("SELECT status FROM users WHERE id = ?", String.class, userId));
        assertNotNull(jdbc.queryForObject("SELECT deleted_at FROM users WHERE id = ?", java.sql.Timestamp.class, userId));
        assertNull(jdbc.queryForObject("SELECT refresh_token FROM users WHERE id = ?", String.class, userId));
    }

    private String membershipStatus(Long meetingId) {
        return jdbc.queryForObject("SELECT status FROM meeting_members WHERE meeting_id = ? AND user_id = ?",
            String.class, meetingId, userId);
    }
}
