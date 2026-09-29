package com.prgms.backend.domain.schedule.integration;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.candidate.service.ScheduleCandidateService;
import com.prgms.backend.domain.schedule.poll.dto.SchedulePollRequest;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.poll.service.SchedulePollCloseService;
import com.prgms.backend.domain.schedule.poll.service.SchedulePollService;
import com.prgms.backend.domain.schedule.scheduler.SchedulePollCloseScheduler;
import com.prgms.backend.domain.schedule.vote.dto.ScheduleVoteRequest;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.domain.schedule.vote.service.ScheduleVoteService;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.notification.repository.NotificationRepository;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateHasVotesException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateLimitExceededException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.sql.Timestamp;
import java.util.UUID;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        "custom.jwt.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"
})
@Testcontainers(disabledWithoutDocker = true)
class ScheduleConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("schedule_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingMemberRepository meetingMemberRepository;
    @MockitoSpyBean private SchedulePollRepository schedulePollRepository;
    @MockitoBean private SchedulePollCloseScheduler closeScheduler;
    @Autowired private ScheduleCandidateRepository scheduleCandidateRepository;
    @Autowired private ScheduleVoteRepository scheduleVoteRepository;
    @Autowired private SchedulePollService schedulePollService;
    @Autowired private SchedulePollCloseService closeService;
    @Autowired private ScheduleCandidateService candidateService;
    @Autowired private ScheduleVoteService voteService;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long meetingId;
    private Long hostId;
    private Long meetingMemberId;

    @BeforeEach
    void setUp() {
        assertEquals("REPEATABLE-READ",
                jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class));
        String suffix = UUID.randomUUID().toString();
        User host = userRepository.save(new User("host-" + suffix + "@example.com", "host-" + suffix, "password"));
        Meeting meeting = meetingRepository.save(new Meeting(host, "일정 테스트", ""));
        MeetingMember member = meetingMemberRepository.save(new MeetingMember(meeting, host));

        meetingId = meeting.getId();
        hostId = host.getId();
        meetingMemberId = member.getId();
        schedulePollService.create(meetingId, hostId,
                new SchedulePollRequest.Create(LocalDateTime.now().plusDays(7)));
    }

    @Test
    @DisplayName("9개일 때 두 후보 추가가 겹쳐도 10개를 넘지 않는다")
    void candidateLimitUnderConcurrentRequests() throws Exception {
        LocalDate firstDate = LocalDate.now().plusDays(1);
        for (int i = 0; i < 9; i++) {
            candidateService.create(meetingId, hostId,
                    new ScheduleCandidateRequest.Create(firstDate.plusDays(i)));
        }

        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = holdTransaction(executor,
                    () -> candidateService.create(meetingId, hostId,
                            new ScheduleCandidateRequest.Create(firstDate.plusDays(9))),
                    firstInserted, releaseFirst);
            await(firstInserted);
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return candidateService.create(meetingId, hostId,
                        new ScheduleCandidateRequest.Create(firstDate.plusDays(10)));
            });

            assertWaiting(secondStarted, second);
            releaseFirst.countDown();
            first.get(20, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> second.get(20, TimeUnit.SECONDS));
            assertInstanceOf(ScheduleCandidateLimitExceededException.class, failure.getCause());
            assertEquals(10, schedulePollRepository.findByMeetingIdWithCandidates(meetingId)
                    .orElseThrow().getCandidates().size());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("투표가 커밋될 때까지 기다린 후보 수정은 투표 존재를 확인하고 거부된다")
    void updateWaitsForVote() throws Exception {
        LocalDate original = LocalDate.now().plusDays(1);
        Long candidateId = candidateService.create(meetingId, hostId,
                new ScheduleCandidateRequest.Create(original)).id();

        CountDownLatch voteInserted = new CountDownLatch(1);
        CountDownLatch releaseVote = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> vote = holdTransaction(executor,
                    () -> voteService.submit(meetingId, candidateId, hostId,
                            new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)),
                    voteInserted, releaseVote);
            await(voteInserted);
            CountDownLatch updateStarted = new CountDownLatch(1);
            Future<?> update = executor.submit(() -> {
                updateStarted.countDown();
                return candidateService.update(meetingId, candidateId, hostId,
                        new ScheduleCandidateRequest.Update(original.plusDays(1)));
            });

            assertWaiting(updateStarted, update);
            releaseVote.countDown();
            vote.get(20, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> update.get(20, TimeUnit.SECONDS));
            assertInstanceOf(ScheduleCandidateHasVotesException.class, failure.getCause());
            assertEquals(original, scheduleCandidateRepository.findById(candidateId)
                    .orElseThrow().getCandidateDate());
        } finally {
            releaseVote.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("투표가 커밋될 때까지 기다린 후보 삭제도 투표 존재를 확인하고 거부된다")
    void deleteWaitsForVote() throws Exception {
        Long candidateId = candidateService.create(meetingId, hostId,
                new ScheduleCandidateRequest.Create(LocalDate.now().plusDays(1))).id();

        CountDownLatch voteInserted = new CountDownLatch(1);
        CountDownLatch releaseVote = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> vote = holdTransaction(executor,
                    () -> voteService.submit(meetingId, candidateId, hostId,
                            new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)),
                    voteInserted, releaseVote);
            await(voteInserted);
            CountDownLatch deleteStarted = new CountDownLatch(1);
            Future<?> delete = executor.submit(() -> {
                deleteStarted.countDown();
                candidateService.delete(meetingId, candidateId, hostId);
            });

            assertWaiting(deleteStarted, delete);
            releaseVote.countDown();
            vote.get(20, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> delete.get(20, TimeUnit.SECONDS));
            assertInstanceOf(ScheduleCandidateHasVotesException.class, failure.getCause());
            assertTrue(scheduleCandidateRepository.existsById(candidateId));
        } finally {
            releaseVote.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 회원의 첫 투표 두 건은 하나의 행으로 저장된다")
    void sameMemberFirstVoteIsSerialized() throws Exception {
        Long candidateId = candidateService.create(meetingId, hostId,
                new ScheduleCandidateRequest.Create(LocalDate.now().plusDays(1))).id();

        CountDownLatch firstInserted = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = holdTransaction(executor,
                    () -> voteService.submit(meetingId, candidateId, hostId,
                            new ScheduleVoteRequest.Submit(SchedulePreference.PREFER)),
                    firstInserted, releaseFirst);
            await(firstInserted);
            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return voteService.submit(meetingId, candidateId, hostId,
                        new ScheduleVoteRequest.Submit(SchedulePreference.AVAILABLE));
            });

            assertWaiting(secondStarted, second);
            releaseFirst.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);
            assertEquals(SchedulePreference.AVAILABLE,
                    scheduleVoteRepository.findByScheduleCandidateIdAndMeetingMemberId(
                            candidateId, meetingMemberId).orElseThrow().getPreference());
            Long pollId = schedulePollRepository.findByMeetingId(meetingId).orElseThrow().getId();
            assertEquals(1, scheduleVoteRepository.findAllByScheduleCandidateSchedulePollId(pollId).size());
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("마감 처리의 첫 조회 이후 가입한 회원도 마감 알림을 받는다")
    void closeNotifiesMembersCommittedAfterInitialRead() throws Exception {
        Long pollId = schedulePollRepository.findByMeetingId(meetingId).orElseThrow().getId();
        String suffix = UUID.randomUUID().toString();
        User newUser = userRepository.save(new User(
                "new-" + suffix + "@example.com", "new-" + suffix, "password"));

        CountDownLatch meetingIdRead = new CountDownLatch(1);
        CountDownLatch continueClose = new CountDownLatch(1);
        doAnswer(invocation -> {
            // 같은 트랜잭션의 연결에서 첫 일반 조회를 실행한 뒤 가입을 끼워 넣는다.
            Long result = jdbcTemplate.queryForObject(
                    "SELECT meeting_id FROM schedule_polls WHERE id = ?", Long.class, pollId);
            assertEquals("READ-COMMITTED",
                    jdbcTemplate.queryForObject("SELECT @@transaction_isolation", String.class));
            meetingIdRead.countDown();
            await(continueClose);
            return Optional.ofNullable(result);
        }).when(schedulePollRepository).findMeetingIdByPollId(pollId);

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> closing = executor.submit(() -> closeService.closeOneIfExpired(pollId));
            if (!meetingIdRead.await(5, TimeUnit.SECONDS)) {
                if (closing.isDone()) {
                    closing.get(1, TimeUnit.SECONDS);
                }
                throw new AssertionError("마감 처리의 첫 조회를 관찰하지 못했습니다.");
            }

            Meeting meeting = meetingRepository.findById(meetingId).orElseThrow();
            meetingMemberRepository.save(new MeetingMember(meeting, newUser));
            jdbcTemplate.update("UPDATE schedule_polls SET deadline = ? WHERE id = ?",
                    Timestamp.valueOf(LocalDateTime.now().minusMinutes(1)), pollId);

            continueClose.countDown();
            closing.get(20, TimeUnit.SECONDS);

            assertEquals(SchedulePollStatus.CLOSED,
                    schedulePollRepository.findById(pollId).orElseThrow().getStatus());
            assertEquals(1, notificationRepository.findByReceiverIdOrderByCreatedAtDesc(newUser.getId()).size());
        } finally {
            continueClose.countDown();
            executor.shutdownNow();
        }
    }

    private Future<?> holdTransaction(ExecutorService executor, Runnable action,
                                      CountDownLatch actionCompleted, CountDownLatch release) {
        return executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
            action.run();
            actionCompleted.countDown();
            await(release);
        }));
    }

    private void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(20, TimeUnit.SECONDS), "동시 요청 준비 시간이 초과됐습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private void assertWaiting(CountDownLatch started, Future<?> request) throws InterruptedException {
        assertTrue(started.await(20, TimeUnit.SECONDS));
        Thread.sleep(150);
        assertFalse(request.isDone(), "첫 번째 트랜잭션이 끝나기 전에 두 번째 요청이 완료됐습니다.");
    }
}
