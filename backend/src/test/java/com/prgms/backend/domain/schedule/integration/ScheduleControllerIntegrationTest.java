package com.prgms.backend.domain.schedule.integration;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.entity.SchedulePreference;
import com.prgms.backend.domain.schedule.vote.entity.ScheduleVote;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.BusinessException;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostRequiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.schedule.*;
import com.prgms.backend.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
        "custom.jwt.access-token-validity-seconds=1800",
        "custom.jwt.refresh-token-validity-seconds=1209600"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class ScheduleControllerIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("schedule_controller_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired MockMvc mvc;
    @Autowired JsonMapper jsonMapper;
    @Autowired JwtTokenProvider tokens;
    @Autowired UserRepository users;
    @Autowired MeetingRepository meetings;
    @Autowired MeetingMemberRepository members;
    @Autowired SchedulePollRepository polls;
    @Autowired ScheduleCandidateRepository candidates;
    @Autowired ScheduleVoteRepository votes;
    @Autowired EntityManager entityManager;

    private User host;
    private User member;
    private User outsider;
    private Meeting meeting;
    private MeetingMember joinedMember;
    private String url;
    private final LocalDateTime deadline = LocalDateTime.now().plusDays(7).withNano(0);
    private final LocalDate candidateDate = LocalDate.now().plusDays(1);

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString();
        host = users.save(new User("host-" + suffix + "@example.com", "host-" + suffix, "password"));
        member = users.save(new User("member-" + suffix + "@example.com", "member-" + suffix, "password"));
        outsider = users.save(new User("outsider-" + suffix + "@example.com", "outsider-" + suffix, "password"));
        meeting = meetings.save(new Meeting(host, "일정 통합 테스트", ""));
        members.save(new MeetingMember(meeting, host));
        joinedMember = members.save(new MeetingMember(meeting, member));
        url = "/api/meetings/" + meeting.getId() + "/schedule-poll";
    }

    @Nested
    @DisplayName("SchedulePollController")
    class PollController {

        @Test
        @DisplayName("모임장은 투표소를 생성하고 201 응답을 받는다")
        void createPoll() throws Exception {
            perform(post(url), host, Map.of("deadline", deadline))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.meetingId").value(meeting.getId()))
                    .andExpect(jsonPath("$.data.deadline").value(deadline.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .andExpect(jsonPath("$.data.status").value("OPEN"));

            flushAndClear();
            assertEquals(deadline, polls.findByMeetingId(meeting.getId()).orElseThrow().getDeadline());
        }

        @Test
        @DisplayName("참여자는 후보를 포함한 투표소를 조회한다")
        void getPoll() throws Exception {
            SchedulePoll poll = createPollFixture();
            ScheduleCandidate candidate = addCandidate(poll, candidateDate);
            perform(get(url), member, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(poll.getId()))
                    .andExpect(jsonPath("$.data.candidates", hasSize(1)))
                    .andExpect(jsonPath("$.data.candidates[0].id").value(candidate.getId()))
                    .andExpect(jsonPath("$.data.candidates[0].candidateDate").value(candidateDate.toString()));
        }

        @Test
        @DisplayName("모임장은 마감 시간을 수정하고 DB에 반영한다")
        void updateDeadline() throws Exception {
            createPollFixture();
            LocalDateTime updated = deadline.plusDays(1);
            perform(patch(url), host, Map.of("deadline", updated))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.deadline").value(updated.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                    .andExpect(jsonPath("$.data.status").value("OPEN"));
            flushAndClear();
            assertEquals(updated, polls.findByMeetingId(meeting.getId()).orElseThrow().getDeadline());
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PATCH"})
        @DisplayName("일반 참여자는 투표소 생성과 마감 시간 수정을 할 수 없다")
        void requiresHost(String method) throws Exception {
            createPollFixture();
            expectBusinessError(perform(request(HttpMethod.valueOf(method), url), member,
                    Map.of("deadline", deadline.plusDays(1))), new MeetingHostRequiredException());
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PATCH"})
        @DisplayName("마감 시간이 누락되거나 과거이면 400으로 거절한다")
        void invalidDeadline(String method) throws Exception {
            createPollFixture();
            expectValidationError(perform(request(HttpMethod.valueOf(method), url), host, Map.of()),
                    "deadline", "마감 시간은 필수입니다.");
            expectValidationError(perform(request(HttpMethod.valueOf(method), url), host,
                    Map.of("deadline", LocalDateTime.now().minusDays(1))),
                    "deadline", "마감 시간은 현재보다 이후여야 합니다.");
            assertEquals(deadline, polls.findByMeetingId(meeting.getId()).orElseThrow().getDeadline());
        }

        @Test
        @DisplayName("투표소를 중복 생성하면 409를 반환한다")
        void duplicatePoll() throws Exception {
            createPollFixture();
            expectBusinessError(perform(post(url), host, Map.of("deadline", deadline)),
                    new SchedulePollAlreadyExistsException(meeting.getId()));
        }

        @Test
        @DisplayName("투표소가 없으면 404를 반환한다")
        void missingPoll() throws Exception {
            expectBusinessError(perform(get(url), member, null),
                    new SchedulePollNotFoundException(meeting.getId()));
        }

        @Test
        @DisplayName("존재하지 않는 모임은 404를 반환한다")
        void missingMeeting() throws Exception {
            expectBusinessError(perform(get("/api/meetings/9223372036854775807/schedule-poll"), host, null),
                    new MeetingNotFoundException(Long.MAX_VALUE));
        }

        @Test
        @DisplayName("모임에 가입하지 않은 사용자는 조회할 수 없다")
        void outsiderCannotRead() throws Exception {
            createPollFixture();
            expectBusinessError(perform(get(url), outsider, null),
                    new MeetingMemberNotFoundException(meeting.getId(), outsider.getId()));
        }

        @Test
        @DisplayName("탈퇴한 참여자는 조회할 수 없다")
        void leftMemberCannotRead() throws Exception {
            createPollFixture();
            joinedMember.leave();
            expectBusinessError(perform(get(url), member, null),
                    new MeetingMemberAlreadyLeftException(meeting.getId(), member.getId()));
        }

        @Test
        @DisplayName("마감한 투표소의 마감 시간은 수정할 수 없다")
        void closedPollCannotUpdate() throws Exception {
            createPollFixture().close();
            expectBusinessError(perform(patch(url), host, Map.of("deadline", deadline.plusDays(1))),
                    new SchedulePollClosedException());
        }

        @Test
        @DisplayName("마감 전에는 결과를 조회할 수 없다")
        void resultsRequireClosedPoll() throws Exception {
            createPollFixture();
            expectBusinessError(perform(get(url + "/results"), member, null),
                    new SchedulePollNotClosedException());
        }

        @Test
        @DisplayName("마감 결과에 후보 순위, 점수, 응답 수와 참여자별 미응답을 포함한다")
        void results() throws Exception {
            SchedulePoll poll = createPollFixture();
            ScheduleCandidate first = addCandidate(poll, candidateDate);
            ScheduleCandidate second = addCandidate(poll, candidateDate.plusDays(1));
            votes.save(ScheduleVote.create(first, joinedMember, SchedulePreference.PREFER));
            poll.close();
            flushAndClear();

            perform(get(url + "/results"), member, null)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.candidateRanks", hasSize(2)))
                    .andExpect(jsonPath("$.data.candidateRanks[0].candidateId").value(first.getId()))
                    .andExpect(jsonPath("$.data.candidateRanks[0].totalScore").value(3))
                    .andExpect(jsonPath("$.data.candidateRanks[0].rank").value(1))
                    .andExpect(jsonPath("$.data.candidateRanks[0].responseCount").value(1))
                    .andExpect(jsonPath("$.data.candidateRanks[0].nonResponseCount").value(1))
                    .andExpect(jsonPath("$.data.candidateRanks[0].preferenceCounts.prefer").value(1))
                    .andExpect(jsonPath("$.data.candidateRanks[1].candidateId").value(second.getId()))
                    .andExpect(jsonPath("$.data.candidateRanks[1].totalScore").value(0))
                    .andExpect(jsonPath("$.data.participantResponses", hasSize(2)))
                    .andExpect(jsonPath("$.data.participantResponses[1].userId").value(member.getId()))
                    .andExpect(jsonPath("$.data.participantResponses[1].answers[0].preference").value("PREFER"))
                    .andExpect(jsonPath("$.data.participantResponses[1].answers[1].preference").value(nullValue()));
        }

        @Test
        @DisplayName("외부 사용자와 탈퇴한 참여자는 마감 결과를 조회할 수 없다")
        void resultsRequireJoinedMember() throws Exception {
            createPollFixture().close();
            expectBusinessError(perform(get(url + "/results"), outsider, null),
                    new MeetingMemberNotFoundException(meeting.getId(), outsider.getId()));
            joinedMember.leave();
            expectBusinessError(perform(get(url + "/results"), member, null),
                    new MeetingMemberAlreadyLeftException(meeting.getId(), member.getId()));
        }
    }

    @Nested
    @DisplayName("ScheduleCandidateController")
    class CandidateController {
        private SchedulePoll poll;
        private ScheduleCandidate candidate;
        private String candidateUrl;

        @BeforeEach
        void setUpCandidate() {
            poll = createPollFixture();
            candidate = addCandidate(poll, candidateDate);
            candidateUrl = url + "/candidates/" + candidate.getId();
        }

        @Test
        @DisplayName("모임장은 후보를 추가하고 201 응답을 받는다")
        void createCandidate() throws Exception {
            LocalDate next = candidateDate.plusDays(1);
            perform(post(url + "/candidates"), host, Map.of("candidateDate", next))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(201))
                    .andExpect(jsonPath("$.data.id").isNumber())
                    .andExpect(jsonPath("$.data.candidateDate").value(next.toString()));
            flushAndClear();
            assertEquals(2, polls.findByMeetingIdWithCandidates(meeting.getId()).orElseThrow().getCandidates().size());
        }

        @Test
        @DisplayName("모임장은 투표 없는 후보를 수정한다")
        void updateCandidate() throws Exception {
            LocalDate updated = candidateDate.plusDays(2);
            perform(patch(candidateUrl), host, Map.of("candidateDate", updated))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.id").value(candidate.getId()))
                    .andExpect(jsonPath("$.data.candidateDate").value(updated.toString()));
            flushAndClear();
            assertEquals(updated, candidates.findById(candidate.getId()).orElseThrow().getCandidateDate());
        }

        @Test
        @DisplayName("모임장은 후보를 삭제하고 본문 없는 204 응답을 받는다")
        void deleteCandidate() throws Exception {
            perform(delete(candidateUrl), host, null)
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));
            flushAndClear();
            assertFalse(candidates.existsById(candidate.getId()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PATCH", "DELETE"})
        @DisplayName("일반 참여자는 후보 추가·수정·삭제를 할 수 없다")
        void requiresHost(String method) throws Exception {
            String target = method.equals("POST") ? url + "/candidates" : candidateUrl;
            expectBusinessError(perform(request(HttpMethod.valueOf(method), target), member,
                    Map.of("candidateDate", candidateDate.plusDays(1))), new MeetingHostRequiredException());
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PATCH"})
        @DisplayName("날짜 누락, 오늘, 과거 날짜를 거절한다")
        void invalidDate(String method) throws Exception {
            String target = method.equals("POST") ? url + "/candidates" : candidateUrl;
            expectValidationError(perform(request(HttpMethod.valueOf(method), target), host, Map.of()),
                    "candidateDate", "날짜는 필수입니다");
            expectValidationError(perform(request(HttpMethod.valueOf(method), target), host,
                    Map.of("candidateDate", LocalDate.now())),
                    "candidateDate", "후보 날짜는 오늘보다 이후여야 합니다.");
            expectValidationError(perform(request(HttpMethod.valueOf(method), target), host,
                    Map.of("candidateDate", LocalDate.now().minusDays(1))),
                    "candidateDate", "후보 날짜는 오늘보다 이후여야 합니다.");
            assertEquals(candidateDate, candidates.findById(candidate.getId()).orElseThrow().getCandidateDate());
        }

        @Test
        @DisplayName("중복 날짜의 후보 추가와 수정을 거절한다")
        void duplicateDate() throws Exception {
            expectBusinessError(perform(post(url + "/candidates"), host, Map.of("candidateDate", candidateDate)),
                    new DuplicateScheduleCandidateException(candidateDate));
            ScheduleCandidate other = addCandidate(poll, candidateDate.plusDays(1));
            expectBusinessError(perform(patch(url + "/candidates/" + other.getId()), host,
                    Map.of("candidateDate", candidateDate)), new DuplicateScheduleCandidateException(candidateDate));
        }

        @Test
        @DisplayName("열한 번째 후보를 추가하면 409를 반환한다")
        void candidateLimit() throws Exception {
            for (int i = 1; i < 10; i++) {
                addCandidate(poll, candidateDate.plusDays(i));
            }
            expectBusinessError(perform(post(url + "/candidates"), host,
                    Map.of("candidateDate", candidateDate.plusDays(10))), new ScheduleCandidateLimitExceededException(10));
        }

        @ParameterizedTest
        @ValueSource(strings = {"PATCH", "DELETE"})
        @DisplayName("투표가 있는 후보는 수정·삭제할 수 없다")
        void candidateWithVotes(String method) throws Exception {
            votes.save(ScheduleVote.create(candidate, joinedMember, SchedulePreference.AVAILABLE));
            expectBusinessError(perform(request(HttpMethod.valueOf(method), candidateUrl), host,
                    Map.of("candidateDate", candidateDate.plusDays(1))),
                    new ScheduleCandidateHasVotesException(candidate.getId()));
            assertTrue(candidates.existsById(candidate.getId()));
            assertEquals(candidateDate, candidates.findById(candidate.getId()).orElseThrow().getCandidateDate());
        }

        @ParameterizedTest
        @ValueSource(strings = {"POST", "PATCH", "DELETE"})
        @DisplayName("마감된 투표소의 후보는 변경할 수 없다")
        void closedPoll(String method) throws Exception {
            poll.close();
            String target = method.equals("POST") ? url + "/candidates" : candidateUrl;
            expectBusinessError(perform(request(HttpMethod.valueOf(method), target), host,
                    Map.of("candidateDate", candidateDate.plusDays(1))), new SchedulePollClosedException());
        }

        @Test
        @DisplayName("다른 모임의 후보 ID로 수정·삭제할 수 없다")
        void candidateFromAnotherMeeting() throws Exception {
            Meeting otherMeeting = meetings.save(new Meeting(host, "다른 모임", ""));
            SchedulePoll otherPoll = polls.save(SchedulePoll.create(otherMeeting, deadline));
            ScheduleCandidate other = addCandidate(otherPoll, candidateDate);
            String target = url + "/candidates/" + other.getId();
            expectBusinessError(perform(patch(target), host, Map.of("candidateDate", candidateDate.plusDays(1))),
                    new ScheduleCandidateNotFoundException(other.getId()));
            expectBusinessError(perform(delete(target), host, null),
                    new ScheduleCandidateNotFoundException(other.getId()));
        }
    }

    @Nested
    @DisplayName("ScheduleVoteController")
    class VoteController {
        private SchedulePoll poll;
        private ScheduleCandidate candidate;
        private String voteUrl;

        @BeforeEach
        void setUpVote() {
            poll = createPollFixture();
            candidate = addCandidate(poll, candidateDate);
            voteUrl = url + "/candidates/" + candidate.getId() + "/vote";
        }

        @ParameterizedTest
        @EnumSource(SchedulePreference.class)
        @DisplayName("참여자는 각 선호도로 투표하고 200 응답을 받는다")
        void submitVote(SchedulePreference preference) throws Exception {
            perform(put(voteUrl), member, Map.of("preference", preference))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.candidateId").value(candidate.getId()))
                    .andExpect(jsonPath("$.data.meetingMemberId").value(joinedMember.getId()))
                    .andExpect(jsonPath("$.data.preference").value(preference.name()));
            flushAndClear();
            assertEquals(preference, votes.findByScheduleCandidateIdAndMeetingMemberId(
                    candidate.getId(), joinedMember.getId()).orElseThrow().getPreference());
        }

        @Test
        @DisplayName("재투표하면 기존 투표를 수정하고 행을 중복 생성하지 않는다")
        void upsertVote() throws Exception {
            perform(put(voteUrl), member, Map.of("preference", "PREFER")).andExpect(status().isOk());
            flushAndClear();
            Long originalId = votes.findByScheduleCandidateIdAndMeetingMemberId(
                    candidate.getId(), joinedMember.getId()).orElseThrow().getId();
            perform(put(voteUrl), member, Map.of("preference", "IMPOSSIBLE"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.preference").value("IMPOSSIBLE"));
            flushAndClear();
            ScheduleVote saved = votes.findByScheduleCandidateIdAndMeetingMemberId(
                    candidate.getId(), joinedMember.getId()).orElseThrow();
            assertEquals(originalId, saved.getId());
            assertEquals(SchedulePreference.IMPOSSIBLE, saved.getPreference());
            assertEquals(1, votes.findAllByScheduleCandidateSchedulePollId(poll.getId()).size());
        }

        @Test
        @DisplayName("선호도가 누락되면 400으로 거절하고 저장하지 않는다")
        void missingPreference() throws Exception {
            expectValidationError(perform(put(voteUrl), member, Map.of()), "preference", "선호도는 필수입니다.");
            assertFalse(votes.existsByScheduleCandidateId(candidate.getId()));
        }

        @Test
        @DisplayName("비참여자와 탈퇴한 참여자는 투표할 수 없다")
        void requiresJoinedMember() throws Exception {
            expectBusinessError(perform(put(voteUrl), outsider, Map.of("preference", "PREFER")),
                    new MeetingMemberNotFoundException(meeting.getId(), outsider.getId()));
            joinedMember.leave();
            expectBusinessError(perform(put(voteUrl), member, Map.of("preference", "PREFER")),
                    new MeetingMemberAlreadyLeftException(meeting.getId(), member.getId()));
        }

        @Test
        @DisplayName("마감 상태의 투표소에는 투표할 수 없다")
        void closedPoll() throws Exception {
            poll.close();
            expectBusinessError(perform(put(voteUrl), member, Map.of("preference", "PREFER")),
                    new SchedulePollClosedException());
            assertFalse(votes.existsByScheduleCandidateId(candidate.getId()));
        }

        @Test
        @DisplayName("OPEN 상태여도 마감 시간이 지나면 투표할 수 없다")
        void expiredPoll() throws Exception {
            // 스케줄러 실행 전 상태를 재현하며 대기나 시스템 시계 변경을 사용하지 않는다.
            entityManager.flush();
            entityManager.createNativeQuery("UPDATE schedule_polls SET deadline = :deadline WHERE id = :id")
                    .setParameter("deadline", LocalDateTime.now().minusMinutes(1))
                    .setParameter("id", poll.getId()).executeUpdate();
            entityManager.clear();
            expectBusinessError(perform(put(voteUrl), member, Map.of("preference", "PREFER")),
                    new SchedulePollClosedException());
        }

        @Test
        @DisplayName("다른 모임의 후보에 투표할 수 없다")
        void candidateFromAnotherMeeting() throws Exception {
            Meeting otherMeeting = meetings.save(new Meeting(host, "다른 모임", ""));
            SchedulePoll otherPoll = polls.save(SchedulePoll.create(otherMeeting, deadline));
            ScheduleCandidate other = addCandidate(otherPoll, candidateDate);
            expectBusinessError(perform(put(url + "/candidates/" + other.getId() + "/vote"), member,
                    Map.of("preference", "PREFER")), new ScheduleCandidateNotFoundException(other.getId()));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "GET", "PATCH", "RESULTS", "CANDIDATE_POST", "CANDIDATE_PATCH", "CANDIDATE_DELETE", "VOTE"})
    @DisplayName("모든 Schedule API는 인증이 없으면 401을 반환한다")
    void requiresAuthentication(String endpoint) throws Exception {
        MockHttpServletRequestBuilder request = switch (endpoint) {
            case "POST" -> post(url);
            case "GET" -> get(url);
            case "PATCH" -> patch(url);
            case "RESULTS" -> get(url + "/results");
            case "CANDIDATE_POST" -> post(url + "/candidates");
            case "CANDIDATE_PATCH" -> patch(url + "/candidates/1");
            case "CANDIDATE_DELETE" -> delete(url + "/candidates/1");
            case "VOTE" -> put(url + "/candidates/1/vote");
            default -> throw new IllegalArgumentException(endpoint);
        };
        // 인증 실패는 MVC 예외 처리 전에 보안 필터에서 직접 응답한다.
        ResultActions result = mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}"));
        expectError(result, 401, "인증이 필요합니다.");
        assertNull(result.andReturn().getResolvedException());
    }

    private SchedulePoll createPollFixture() {
        return polls.save(SchedulePoll.create(meeting, deadline));
    }

    private ScheduleCandidate addCandidate(SchedulePoll poll, LocalDate date) {
        return candidates.save(poll.addCandidate(date));
    }

    private ResultActions perform(MockHttpServletRequestBuilder request, User user, Object body) throws Exception {
        // 요청 전에 픽스처를 DB에 반영해 신규 엔티티의 컬렉션 상태가 API 동작에 섞이지 않게 한다.
        entityManager.flush();
        request.header("Authorization", "Bearer " + tokens.createAccessToken(user.getId()));
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        return mvc.perform(request);
    }

    private void expectBusinessError(ResultActions result, BusinessException expected) throws Exception {
        result.andExpect(mvcResult -> {
            Exception actual = mvcResult.getResolvedException();
            assertNotNull(actual, "정의한 비즈니스 예외가 발생해야 한다");
            assertEquals(expected.getClass(), actual.getClass(), "다른 원인의 예외로 통과하면 안 된다");
            assertEquals(expected.getMessage(), actual.getMessage());
        });
        expectError(result, expected.getCode(), expected.getMessage());
    }

    private void expectValidationError(ResultActions result, String field, String message) throws Exception {
        result.andExpect(mvcResult -> {
            MethodArgumentNotValidException exception = assertInstanceOf(
                    MethodArgumentNotValidException.class, mvcResult.getResolvedException());
            var fieldError = exception.getBindingResult().getFieldError(field);
            assertNotNull(fieldError, "요청한 필드에서 검증 오류가 발생해야 한다");
            assertEquals(message, fieldError.getDefaultMessage());
        });
        expectError(result, 400, message);
    }

    private void expectError(ResultActions result, int code, String message) throws Exception {
        result.andExpect(status().is(code))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").value(message))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
