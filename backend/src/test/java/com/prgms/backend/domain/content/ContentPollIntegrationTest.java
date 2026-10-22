package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollResultSort;
import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.dto.request.ContentVoteRequest;
import com.prgms.backend.domain.content.dto.response.ContentPollResultsResponse;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.integration.ContentNotificationWriter;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.content.service.ContentCandidateService;
import com.prgms.backend.domain.content.service.ContentPollCloseScheduler;
import com.prgms.backend.domain.content.service.ContentPollCloseService;
import com.prgms.backend.domain.content.service.ContentPollService;
import com.prgms.backend.domain.content.service.ContentVoteService;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingService;
import com.prgms.backend.domain.notification.ENUM.NotificationType;
import com.prgms.backend.domain.notification.repository.NotificationRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingPollNotCompletedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

// 콘텐츠 투표(투표판/후보/투표/마감/확정/결과) 전체 흐름을 실제 MySQL로 검증하는 통합테스트
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
})
@Testcontainers(disabledWithoutDocker = true)
class ContentPollIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("content_flow_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    // 백그라운드 스케줄링이 테스트 데이터를 먼저 닫지 못하게 막고, 스케줄러는 직접 만들어 호출
    @MockitoBean private ContentPollCloseScheduler backgroundScheduler;
    @MockitoSpyBean private ContentNotificationWriter notificationWriter;

    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingMemberRepository meetingMemberRepository;
    @Autowired private ContentPollRepository pollRepository;
    @Autowired private ContentCandidateRepository candidateRepository;
    @Autowired private ContentVoteRepository voteRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ContentPollService pollService;
    @Autowired private ContentCandidateService candidateService;
    @Autowired private ContentVoteService voteService;
    @Autowired private ContentPollCloseService closeService;
    @Autowired private MeetingService meetingService;

    private User host;
    private User alice;
    private User bob;
    private User outsider;
    private Meeting meeting;
    private Long meetingId;
    private MeetingMember hostMember;
    private MeetingMember aliceMember;
    private MeetingMember bobMember;

    @BeforeEach
    void setUp() {
        host = saveUser("host");
        alice = saveUser("alice");
        bob = saveUser("bob");
        outsider = saveUser("outsider");
        meeting = meetingRepository.save(new Meeting(host, "콘텐츠 통합 테스트", ""));
        meetingId = meeting.getId();
        hostMember = meetingMemberRepository.save(new MeetingMember(meeting, host));
        aliceMember = meetingMemberRepository.save(new MeetingMember(meeting, alice));
        bobMember = meetingMemberRepository.save(new MeetingMember(meeting, bob));
    }

    // ---------- 투표판 생성 / 조회 ----------

    // ---------- 후보 ----------

    // ---------- 투표 ----------

    @Test
    @DisplayName("같은 사람이 같은 후보에 동시에 투표해도 표는 한 개만 남음")
    void concurrentVoteLeavesSingleRow() throws Exception {
        openPoll();
        ContentCandidate a = candidate("A", aliceMember);
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch go = new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                ContentPreference preference = i % 2 == 0 ? ContentPreference.PREFER : ContentPreference.DISLIKE;
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    go.await(10, TimeUnit.SECONDS);
                    try {
                        voteService.upsert(meetingId, bob.getId(),
                                new ContentVoteRequest.Submit(a.getId(), preference));
                        return true;
                    } catch (RuntimeException e) {
                        // 동시에 첫 표를 넣다 UNIQUE에 걸린 쪽은 실패할 수 있음
                        return false;
                    }
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            go.countDown();
            long succeeded = 0;
            for (Future<Boolean> future : futures) {
                if (future.get(30, TimeUnit.SECONDS)) succeeded++;
            }

            assertTrue(succeeded >= 1);
            assertEquals(1, voteRepository.findByContentCandidate_ContentPoll_Id(a.getContentPoll().getId()).size());
        } finally {
            go.countDown();
            executor.shutdownNow();
        }
    }

    // ---------- 마감기한 수정 ----------

    // ---------- 스케줄러 마감 + 알림 ----------

    @Test
    @DisplayName("스케줄러가 마감기한 지난 투표판만 닫고 모든 모임원에게 알림을 한 번씩 저장")
    void schedulerClosesExpiredPollAndNotifiesMembers() {
        ContentPoll expired = pollRepository.save(new ContentPoll(meetingId, LocalDateTime.now().minusMinutes(1)));
        Meeting otherMeeting = meetingRepository.save(new Meeting(host, "마감 전 모임", ""));
        ContentPoll notYet = pollRepository.save(new ContentPoll(otherMeeting.getId(), LocalDateTime.now().plusDays(1)));
        long before = contentClosedNotifications(alice.getId());

        scheduler().closeExpired();

        assertEquals(ContentPollStatus.CLOSED, pollRepository.findById(expired.getId()).orElseThrow().getStatus());
        assertEquals(ContentPollStatus.OPEN, pollRepository.findById(notYet.getId()).orElseThrow().getStatus());
        assertEquals(before + 1, contentClosedNotifications(alice.getId()));
        assertEquals(1, contentClosedNotifications(host.getId()));
        assertEquals(1, contentClosedNotifications(bob.getId()));

        // 이미 닫힌 투표판은 다시 닫히거나 알림이 중복 저장되지 않음
        scheduler().closeExpired();
        assertEquals(before + 1, contentClosedNotifications(alice.getId()));
    }

    @Test
    @DisplayName("알림 저장이 실패해도 투표판 마감은 되돌아가지 않음")
    void closeSurvivesNotificationFailure() {
        ContentPoll expired = pollRepository.save(new ContentPoll(meetingId, LocalDateTime.now().minusMinutes(1)));
        doThrow(new IllegalStateException("알림 저장 실패")).when(notificationWriter).save(anyLong());

        closeService.closeOneIfExpired(expired.getId());

        assertEquals(ContentPollStatus.CLOSED, pollRepository.findById(expired.getId()).orElseThrow().getStatus());
        assertEquals(0, contentClosedNotifications(alice.getId()));
    }

    // ---------- 확정 ----------

    // ---------- 모임 종료와의 관계 ----------

    @Test
    @DisplayName("열린 콘텐츠 투표판이 있으면 모임을 종료할 수 없고, 마감 뒤에는 종료할 수 있음")
    void completeMeetingBlockedByOpenPoll() {
        ContentPoll poll = openPoll();

        assertThrows(MeetingPollNotCompletedException.class,
                () -> meetingService.completeMeeting(meetingId, host.getId()));

        close(poll);
        meetingService.completeMeeting(meetingId, host.getId());
        assertTrue(!meetingRepository.findById(meetingId).orElseThrow().isActive());
    }

    // ---------- 결과 ----------

    @Test
    @DisplayName("마감 후 탈퇴한 사람의 표도 참여 인원/미응답 수/인원별 결과에 포함되어 합계가 맞음")
    void withdrawnVoterIsCountedConsistently() {
        ContentPoll poll = openPoll();
        ContentCandidate a = candidate("A", hostMember);
        ContentCandidate b = candidate("B", bobMember);
        vote(a, hostMember, ContentPreference.PREFER);
        vote(a, aliceMember, ContentPreference.AVAILABLE);
        vote(a, bobMember, ContentPreference.DISLIKE);
        vote(b, hostMember, ContentPreference.AVAILABLE);
        vote(b, aliceMember, ContentPreference.PREFER);
        vote(b, bobMember, ContentPreference.PREFER);
        close(poll);
        leave(bobMember);

        ContentPollResultsResponse.Detail results =
                pollService.getResults(meetingId, host.getId(), ContentPollResultSort.SCORE);

        assertEquals(3, results.joinedCount());
        assertEquals(3, results.members().size());
        for (ContentPollResultsResponse.CandidateResult candidate : results.candidates()) {
            assertEquals(3, candidate.responseCount());
            assertEquals(0, candidate.noResponseCount());
        }
        assertTrue(results.members().stream().anyMatch(m -> m.meetingMemberId().equals(bobMember.getId())));
        // 후보 등록자가 탈퇴해도 닉네임은 유지
        ContentPollResultsResponse.CandidateResult byBob = results.candidates().stream()
                .filter(c -> c.candidateId().equals(b.getId())).findFirst().orElseThrow();
        assertTrue(byBob.createdByNickname().startsWith("bob"));
    }

    @Test
    @DisplayName("표 없이 탈퇴한 사람은 미응답으로 잡히지 않고, 미응답 수는 음수가 되지 않음")
    void silentWithdrawnMemberIsNotCounted() {
        ContentPoll poll = openPoll();
        ContentCandidate a = candidate("A", hostMember);
        vote(a, bobMember, ContentPreference.PREFER);
        close(poll);
        leave(bobMember);
        leave(aliceMember);

        ContentPollResultsResponse.Detail results =
                pollService.getResults(meetingId, host.getId(), ContentPollResultSort.SCORE);

        // 호스트(미응답) + 표를 낸 탈퇴자 = 2명, 표 없이 나간 alice는 제외
        assertEquals(2, results.joinedCount());
        ContentPollResultsResponse.CandidateResult result = results.candidates().get(0);
        assertEquals(1, result.responseCount());
        assertEquals(1, result.noResponseCount());
        assertTrue(results.members().stream().noneMatch(m -> m.meetingMemberId().equals(aliceMember.getId())));
    }

    // ---------- helpers ----------

    private ContentPollCloseScheduler scheduler() {
        return new ContentPollCloseScheduler(pollRepository, closeService);
    }

    private long contentClosedNotifications(Long receiverId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(receiverId).stream()
                .filter(n -> n.getMeetingId().equals(meetingId) && n.getType() == NotificationType.CONTENT_CLOSED)
                .count();
    }

    private ContentPoll openPoll() {
        return pollRepository.save(new ContentPoll(meetingId, LocalDateTime.now().plusDays(1)));
    }

    private void close(ContentPoll poll) {
        ContentPoll stored = pollRepository.findById(poll.getId()).orElseThrow();
        stored.close();
        pollRepository.save(stored);
    }

    private ContentCandidate candidate(String title, MeetingMember creator) {
        ContentPoll poll = pollRepository.findByMeetingId(meetingId).orElseThrow();
        return candidateRepository.save(new ContentCandidate(poll, creator.getId(), title, ""));
    }

    private void vote(ContentCandidate candidate, MeetingMember member, ContentPreference preference) {
        voteRepository.save(new ContentVote(candidate, member.getId(), preference));
    }

    private void leave(MeetingMember member) {
        MeetingMember stored = meetingMemberRepository.findById(member.getId()).orElseThrow();
        stored.leave();
        meetingMemberRepository.save(stored);
    }

    private User saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(new User(prefix + "-" + suffix + "@example.com", prefix + "-" + suffix, "password"));
    }
}
