package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingInvitationService;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.meeting.AlreadyMeetingMemberException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;

@DataJpaTest(showSql = false, properties = {
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.datasource.hikari.transaction-isolation=TRANSACTION_REPEATABLE_READ",
    "logging.level.org.hibernate.SQL=OFF"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(MeetingInvitationService.class)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@Testcontainers(disabledWithoutDocker = true)
class MeetingInvitationConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
        .withDatabaseName("invitation_join_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private UserRepository users;
    @Autowired private MeetingRepository meetings;
    @Autowired private MeetingInvitationRepository invitations;
    @MockitoSpyBean private MeetingMemberRepository members;
    @Autowired private MeetingInvitationService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private TransactionTemplate transactions;

    private Meeting meeting;
    private User user;
    private String inviteCode;

    @BeforeEach
    void setUp() {
        User host = createUser();
        user = createUser();
        meeting = meetings.saveAndFlush(new Meeting(host, "초대 가입 동시성 테스트", ""));
        inviteCode = createInvitation();
    }

    @Test
    @DisplayName("같은 사용자와 같은 초대 코드로 동시에 가입하면 한 번 성공하고 나머지는 409")
    void sameUserAndSameCode() throws Exception {
        JoinRequest request = new JoinRequest(inviteCode, user.getId());
        List<JoinResult> results = joinConcurrently(List.of(request, request, request, request));

        assertResponses(results, 1, 3);
        assertOneMembership(user.getId());
    }

    @Test
    @DisplayName("같은 모임의 서로 다른 초대 코드로 동시 가입해도 모임원은 한 행만 유지")
    void sameUserAndDifferentCodes() throws Exception {
        String otherCode = createInvitation();
        List<JoinResult> results = joinConcurrently(List.of(
            new JoinRequest(inviteCode, user.getId()),
            new JoinRequest(otherCode, user.getId())));

        assertResponses(results, 1, 1);
        assertOneMembership(user.getId());
    }

    @Test
    @DisplayName("서로 다른 사용자가 같은 초대 코드로 동시 가입하면 모두 정상 가입")
    void differentUsersAndSameCode() throws Exception {
        User otherUser = createUser();
        List<JoinResult> results = joinConcurrently(List.of(
            new JoinRequest(inviteCode, user.getId()),
            new JoinRequest(inviteCode, otherUser.getId())));

        assertResponses(results, 2, 0);
        assertOneMembership(user.getId());
        assertOneMembership(otherUser.getId());
    }

    @Test
    @DisplayName("LEFT 모임원이 동시에 재가입하면 기존 ID를 재사용하고 한 요청만 성공")
    void rejoinReusesMemberId() throws Exception {
        MeetingMember member = new MeetingMember(meeting, user);
        member.leave();
        Long originalId = members.saveAndFlush(member).getId();
        JoinRequest request = new JoinRequest(inviteCode, user.getId());

        List<JoinResult> results = joinConcurrently(List.of(request, request));

        assertResponses(results, 1, 1);
        MeetingMember rejoined = members.findById(originalId).orElseThrow();
        assertEquals(MeetingMemberStatus.JOINED, rejoined.getStatus());
        assertEquals(originalId, results.stream().filter(result -> result.code() == 200)
            .findFirst().orElseThrow().memberId());
        assertOneMembership(user.getId());
    }

    @Test
    @DisplayName("실제 MySQL 모임원 UNIQUE 위반도 409 예외로 변환하고 기존 행을 유지")
    void realDuplicateKeyBecomesConflict() {
        Long originalId = members.saveAndFlush(new MeetingMember(meeting, user)).getId();
        // 사전 조회로 걸러지지 않은 상황을 만들어 실제 INSERT 제약 위반 경로를 검증
        doReturn(Optional.empty()).when(members)
            .findByMeetingIdAndUserIdForUpdate(meeting.getId(), user.getId());

        AlreadyMeetingMemberException exception = assertThrows(AlreadyMeetingMemberException.class,
            () -> service.joinMeeting(inviteCode, user.getId()));

        assertEquals(409, exception.getCode());
        assertEquals(originalId, members.findByMeetingIdAndUserId(meeting.getId(), user.getId())
            .orElseThrow().getId());
        assertOneMembership(user.getId());
    }

    private List<JoinResult> joinConcurrently(List<JoinRequest> requests) throws Exception {
        CyclicBarrier snapshotsCreated = new CyclicBarrier(requests.size());
        var executor = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<JoinResult>> futures = new ArrayList<>();
            for (JoinRequest request : requests) {
                futures.add(executor.submit(() -> {
                    try {
                        MeetingMemberResponse response = transactions.execute(status -> {
                            // 각 트랜잭션에서 실제 조회로 스냅샷을 만든 뒤 잠금 경쟁을 시작
                            invitations.findMeetingIdByInviteCode(request.code()).orElseThrow();
                            assertDoesNotThrow(() -> snapshotsCreated.await(10, TimeUnit.SECONDS));
                            return service.joinMeeting(request.code(), request.userId());
                        });
                        return new JoinResult(200, response.id());
                    } catch (AlreadyMeetingMemberException e) {
                        return new JoinResult(e.getCode(), null);
                    }
                }));
            }
            List<JoinResult> results = new ArrayList<>();
            for (Future<JoinResult> future : futures) {
                results.add(future.get(20, TimeUnit.SECONDS));
            }
            return results;
        } finally {
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void assertResponses(List<JoinResult> results, long successes, long conflicts) {
        assertEquals(successes, results.stream().filter(result -> result.code() == 200).count());
        assertEquals(conflicts, results.stream().filter(result -> result.code() == 409).count());
    }

    private void assertOneMembership(Long userId) {
        assertEquals(1L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM meeting_members WHERE meeting_id = ? AND user_id = ?",
            Long.class, meeting.getId(), userId));
    }

    private User createUser() {
        String suffix = UUID.randomUUID().toString();
        return users.saveAndFlush(new User(suffix + "@test.com", suffix, "password"));
    }

    private String createInvitation() {
        String code = UUID.randomUUID().toString();
        invitations.saveAndFlush(new MeetingInvitation(meeting, code, LocalDateTime.now().plusDays(1)));
        return code;
    }

    private record JoinRequest(String code, Long userId) {}
    private record JoinResult(int code, Long memberId) {}
}
