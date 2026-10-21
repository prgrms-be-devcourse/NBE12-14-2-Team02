package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.dto.request.ContentPollRequest;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.integration.ContentNotificationWriter;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.service.ContentPollCloseScheduler;
import com.prgms.backend.domain.content.service.ContentPollCloseService;
import com.prgms.backend.domain.content.service.ContentPollService;
import com.prgms.backend.global.exception.custom.content.ContentPollAlreadyConfirmedException;
import com.prgms.backend.global.exception.custom.content.ContentPollClosedException;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
})
@Testcontainers(disabledWithoutDocker = true)
class ContentPollCloseConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("content_concurrency_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    // 백그라운드 스케줄링이 테스트 데이터를 먼저 닫지 못하게 막음
    @MockitoBean private ContentPollCloseScheduler closeScheduler;
    // 마감 이벤트로 알림 저장이 몇 번 호출되는지 확인
    @MockitoSpyBean private ContentNotificationWriter notificationWriter;

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private ContentPollRepository contentPollRepository;
    @Autowired private ContentCandidateRepository contentCandidateRepository;
    @Autowired private ContentPollCloseService closeService;
    @Autowired private ContentPollService contentPollService;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long hostId;
    private Long meetingId;
    private Long pollId;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        User host = userRepository.save(new User("host-" + suffix + "@example.com", "host-" + suffix, "password"));
        hostId = host.getId();
        meetingId = meetingRepository.save(new Meeting(host, "콘텐츠 동시성 테스트", "")).getId();
        pollId = contentPollRepository.save(new ContentPoll(meetingId, LocalDateTime.now().minusMinutes(1))).getId();
    }

    @Test
    @DisplayName("만료 투표판 조회는 ACTIVE 모임의 마감기한 지난 OPEN 투표판만 대상")
    void findExpiredIdsFiltersByPollAndMeeting() {
        User otherHost = userRepository.save(new User("other-" + UUID.randomUUID() + "@example.com",
                "other-" + UUID.randomUUID(), "password"));

        Meeting activeNotYet = meetingRepository.save(new Meeting(otherHost, "마감 전", ""));
        Long notYetId = contentPollRepository.save(
                new ContentPoll(activeNotYet.getId(), LocalDateTime.now().plusDays(1))).getId();

        Meeting completed = meetingRepository.save(new Meeting(otherHost, "종료", ""));
        Long completedId = contentPollRepository.save(
                new ContentPoll(completed.getId(), LocalDateTime.now().minusMinutes(1))).getId();
        completed.complete();
        meetingRepository.save(completed);

        Meeting deleted = meetingRepository.save(new Meeting(otherHost, "삭제", ""));
        Long deletedId = contentPollRepository.save(
                new ContentPoll(deleted.getId(), LocalDateTime.now().minusMinutes(1))).getId();
        deleted.softDelete();
        meetingRepository.save(deleted);

        var ids = contentPollRepository.findExpiredIds(
                ContentPollStatus.OPEN, MeetingStatus.ACTIVE, LocalDateTime.now());

        assertTrue(ids.contains(pollId));
        assertFalse(ids.contains(notYetId));
        assertFalse(ids.contains(completedId));
        assertFalse(ids.contains(deletedId));
    }

    @Test
    @DisplayName("같은 투표판을 동시에 마감해도 한 번만 닫고 알림도 한 번만 저장")
    void concurrentCloseNotifiesOnce() throws Exception {
        CountDownLatch firstClosed = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // 첫 번째 마감: 투표판 락을 잡은 채 커밋 직전에 대기
            Future<?> first = holdTransaction(executor,
                    () -> closeService.closeOneIfExpired(pollId), firstClosed, releaseFirst);
            await(firstClosed);

            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                closeService.closeOneIfExpired(pollId);
            });

            assertWaiting(secondStarted, second);
            releaseFirst.countDown();
            first.get(20, TimeUnit.SECONDS);
            second.get(20, TimeUnit.SECONDS);

            assertEquals(ContentPollStatus.CLOSED,
                    contentPollRepository.findById(pollId).orElseThrow().getStatus());
            verify(notificationWriter, times(1)).save(meetingId);
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("투표판 락을 잡은 쪽이 마감기한을 연장하면 기다린 마감은 재검증 후 건너뜀")
    void closeWaitsForPollLockAndSkipsExtendedDeadline() throws Exception {
        CountDownLatch deadlineChanged = new CountDownLatch(1);
        CountDownLatch releaseUpdate = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> update = holdTransaction(executor, () -> {
                ContentPoll poll = contentPollRepository.findByIdForUpdate(pollId).orElseThrow();
                poll.changeDeadLine(LocalDateTime.now().plusDays(1));
            }, deadlineChanged, releaseUpdate);
            await(deadlineChanged);

            CountDownLatch closeStarted = new CountDownLatch(1);
            Future<?> close = executor.submit(() -> {
                closeStarted.countDown();
                closeService.closeOneIfExpired(pollId);
            });

            assertWaiting(closeStarted, close);
            releaseUpdate.countDown();
            update.get(20, TimeUnit.SECONDS);
            close.get(20, TimeUnit.SECONDS);

            assertEquals(ContentPollStatus.OPEN,
                    contentPollRepository.findById(pollId).orElseThrow().getStatus());
            verify(notificationWriter, times(0)).save(meetingId);
        } finally {
            releaseUpdate.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("모임 락을 잡은 쪽이 모임을 종료하면 기다린 마감은 재검증 후 건너뜀")
    void closeWaitsForMeetingLockAndSkipsCompletedMeeting() throws Exception {
        CountDownLatch meetingCompleted = new CountDownLatch(1);
        CountDownLatch releaseComplete = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> complete = holdTransaction(executor, () -> {
                Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNullForUpdate(meetingId).orElseThrow();
                meeting.complete();
            }, meetingCompleted, releaseComplete);
            await(meetingCompleted);

            CountDownLatch closeStarted = new CountDownLatch(1);
            Future<?> close = executor.submit(() -> {
                closeStarted.countDown();
                closeService.closeOneIfExpired(pollId);
            });

            assertWaiting(closeStarted, close);
            releaseComplete.countDown();
            complete.get(20, TimeUnit.SECONDS);
            close.get(20, TimeUnit.SECONDS);

            assertEquals(ContentPollStatus.OPEN,
                    contentPollRepository.findById(pollId).orElseThrow().getStatus());
            verify(notificationWriter, times(0)).save(meetingId);
        } finally {
            releaseComplete.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("마감이 투표판 락을 쥐고 있으면 마감기한 수정은 기다렸다가 마감된 투표판이라 거부")
    void updateDeadlineWaitsForCloseAndIsRejected() throws Exception {
        CountDownLatch closed = new CountDownLatch(1);
        CountDownLatch releaseClose = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> close = holdTransaction(executor,
                    () -> closeService.closeOneIfExpired(pollId), closed, releaseClose);
            await(closed);

            CountDownLatch updateStarted = new CountDownLatch(1);
            Future<?> update = executor.submit(() -> {
                updateStarted.countDown();
                return contentPollService.updateDeadline(meetingId, hostId,
                        new ContentPollRequest.UpdateDeadline(LocalDateTime.now().plusDays(1)));
            });

            assertWaiting(updateStarted, update);
            releaseClose.countDown();
            close.get(20, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> update.get(20, TimeUnit.SECONDS));
            assertInstanceOf(ContentPollClosedException.class, failure.getCause());
            assertEquals(ContentPollStatus.CLOSED,
                    contentPollRepository.findById(pollId).orElseThrow().getStatus());
        } finally {
            releaseClose.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    @DisplayName("같은 후보 확정 요청이 겹치면 두 번째는 기다렸다가 이미 확정됨으로 거부")
    void concurrentConfirmIsSerialized() throws Exception {
        closeService.closeOneIfExpired(pollId);
        ContentPoll poll = contentPollRepository.findById(pollId).orElseThrow();
        Long firstCandidateId = contentCandidateRepository.save(
                new ContentCandidate(poll, 1L, "후보1", "")).getId();
        Long secondCandidateId = contentCandidateRepository.save(
                new ContentCandidate(poll, 1L, "후보2", "")).getId();

        CountDownLatch firstConfirmed = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = holdTransaction(executor,
                    () -> contentPollService.confirm(meetingId, hostId,
                            new ContentPollRequest.Confirm(firstCandidateId)),
                    firstConfirmed, releaseFirst);
            await(firstConfirmed);

            CountDownLatch secondStarted = new CountDownLatch(1);
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                return contentPollService.confirm(meetingId, hostId,
                        new ContentPollRequest.Confirm(secondCandidateId));
            });

            assertWaiting(secondStarted, second);
            releaseFirst.countDown();
            first.get(20, TimeUnit.SECONDS);
            ExecutionException failure = assertThrows(ExecutionException.class,
                    () -> second.get(20, TimeUnit.SECONDS));
            assertInstanceOf(ContentPollAlreadyConfirmedException.class, failure.getCause());
            assertEquals(firstCandidateId,
                    contentPollRepository.findById(pollId).orElseThrow().getConfirmedCandidateId());
        } finally {
            releaseFirst.countDown();
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
