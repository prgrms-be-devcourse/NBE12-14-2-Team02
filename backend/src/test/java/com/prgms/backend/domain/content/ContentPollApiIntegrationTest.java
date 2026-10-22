package com.prgms.backend.domain.content;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.ENUM.ContentPreference;
import com.prgms.backend.domain.content.entity.ContentCandidate;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.entity.ContentVote;
import com.prgms.backend.domain.content.repository.ContentCandidateRepository;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.content.service.ContentPollCloseScheduler;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import java.time.LocalDateTime;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 보안 필터(JWT) → 컨트롤러 → 서비스 → MySQL → 예외 핸들러까지 실제로 연결해 HTTP 응답 코드를 검증
@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create",
        "spring.jpa.show-sql=false",
        "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
        "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
})
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class ContentPollApiIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
            .withDatabaseName("content_api_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @MockitoBean private ContentPollCloseScheduler backgroundScheduler;

    @Autowired private MockMvc mvc;
    @Autowired private JwtTokenProvider jwt;
    @Autowired private UserRepository userRepository;
    @Autowired private MeetingRepository meetingRepository;
    @Autowired private MeetingMemberRepository meetingMemberRepository;
    @Autowired private ContentPollRepository pollRepository;
    @Autowired private ContentCandidateRepository candidateRepository;
    @Autowired private ContentVoteRepository voteRepository;

    private User host;
    private User alice;
    private User outsider;
    private Long meetingId;
    private MeetingMember aliceMember;
    private String base;

    @BeforeEach
    void setUp() {
        host = saveUser("host");
        alice = saveUser("alice");
        outsider = saveUser("outsider");
        Meeting meeting = meetingRepository.save(new Meeting(host, "API 통합 테스트", ""));
        meetingId = meeting.getId();
        meetingMemberRepository.save(new MeetingMember(meeting, host));
        aliceMember = meetingMemberRepository.save(new MeetingMember(meeting, alice));
        base = "/api/meetings/" + meetingId;
    }

    // ---------- 인증 ----------

    @Test
    @DisplayName("토큰이 없으면 모든 콘텐츠 API가 401")
    void unauthenticated() throws Exception {
        mvc.perform(get(base + "/content-poll")).andExpect(status().isUnauthorized());
        mvc.perform(post(base + "/content-poll").contentType(MediaType.APPLICATION_JSON).content(deadlineJson(1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(base + "/content-poll/results")).andExpect(status().isUnauthorized());
        mvc.perform(put(base + "/content-votes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("탈퇴한 회원의 토큰은 발급 이후라도 401")
    void withdrawnUserToken() throws Exception {
        openPoll();
        alice.updateStatus(UserStatus.WITHDRAWN);
        alice.updateDeletedAt(LocalDateTime.now());
        userRepository.save(alice);

        mvc.perform(auth(get(base + "/content-poll"), alice)).andExpect(status().isUnauthorized());
    }

    // ---------- 투표판 ----------

    @Test
    @DisplayName("투표판 생성: 비모임장 403, 과거 마감시간 400, 모임장 201, 중복 409")
    void createPoll() throws Exception {
        mvc.perform(auth(post(base + "/content-poll"), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(deadlineJson(1)))
                .andExpect(status().isForbidden());
        mvc.perform(auth(post(base + "/content-poll"), host).contentType(MediaType.APPLICATION_JSON)
                        .content(deadlineJson(-1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        mvc.perform(auth(post(base + "/content-poll"), host).contentType(MediaType.APPLICATION_JSON)
                        .content(deadlineJson(1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("OPEN"));

        mvc.perform(auth(post(base + "/content-poll"), host).contentType(MediaType.APPLICATION_JSON)
                        .content(deadlineJson(2)))
                .andExpect(status().isConflict());
    }

    // ---------- 후보 ----------

    @Test
    @DisplayName("후보: 생성 201, 중복 제목 409, 제목 누락 400, 수정 200, 삭제 204")
    void candidateLifecycle() throws Exception {
        openPoll();
        String path = base + "/content-poll/candidates";

        mvc.perform(auth(post(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"영화\",\"description\":\"설명\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.title").value("영화"));
        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"영화\"}"))
                .andExpect(status().isConflict());
        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());

        ContentCandidate saved = candidateRepository.findAll().stream()
                .filter(c -> c.getTitle().equals("영화")).findFirst().orElseThrow();
        mvc.perform(auth(patch(path + "/" + saved.getId()), host).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"뺏기\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(auth(patch(path + "/" + saved.getId()), alice).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"드라마\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("드라마"));
        mvc.perform(auth(delete(path + "/" + saved.getId()), alice)).andExpect(status().isNoContent());
        mvc.perform(auth(delete(path + "/" + saved.getId()), alice)).andExpect(status().isNotFound());
    }

    // ---------- 투표 ----------

    @Test
    @DisplayName("투표: 200, 같은 후보 재투표는 선택만 변경, 본문 누락 400, 마감 후 409")
    void vote() throws Exception {
        ContentPoll poll = openPoll();
        ContentCandidate candidate = candidate("A", aliceMember);
        String path = base + "/content-votes";

        mvc.perform(auth(put(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson(candidate.getId(), "PREFER")))
                .andExpect(status().isOk());
        mvc.perform(auth(put(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson(candidate.getId(), "DISLIKE")))
                .andExpect(status().isOk());
        assertEquals(1, voteRepository.findByContentCandidate_ContentPoll_Id(poll.getId()).size());

        mvc.perform(auth(put(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"candidateId\":" + candidate.getId() + "}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(put(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson(99999L, "PREFER")))
                .andExpect(status().isNotFound());

        close(poll);
        mvc.perform(auth(put(path), alice).contentType(MediaType.APPLICATION_JSON)
                        .content(voteJson(candidate.getId(), "PREFER")))
                .andExpect(status().isConflict());
    }

    // ---------- 결과 / 확정 ----------

    @Test
    @DisplayName("결과: 진행 중 409, 마감 후 200이며 정렬 파라미터 적용")
    void results() throws Exception {
        ContentPoll poll = openPoll();
        ContentCandidate candidate = candidate("A", aliceMember);
        voteRepository.save(new ContentVote(candidate, aliceMember.getId(), ContentPreference.PREFER));
        String path = base + "/content-poll/results";

        mvc.perform(auth(get(path), alice)).andExpect(status().isConflict());

        close(poll);
        mvc.perform(auth(get(path), alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.candidates[0].responseCount").value(1))
                .andExpect(jsonPath("$.data.candidates[0].noResponseCount").value(1));
        mvc.perform(auth(get(path).param("sort", "PARTICIPANTS"), alice)).andExpect(status().isOk());
        mvc.perform(auth(get(path), outsider)).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("확정: 진행 중 409, 비모임장 403, 모임장 200, 재확정 409, 후보 누락 400")
    void confirm() throws Exception {
        ContentPoll poll = openPoll();
        ContentCandidate candidate = candidate("A", aliceMember);
        String path = base + "/content-poll/confirm";
        String body = "{\"candidateId\":" + candidate.getId() + "}";

        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        close(poll);
        mvc.perform(auth(post(path), alice).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(auth(post(path), host).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        assertEquals(candidate.getId(),
                pollRepository.findById(poll.getId()).orElseThrow().getConfirmedCandidateId());
        assertEquals(ContentPollStatus.CLOSED, pollRepository.findById(poll.getId()).orElseThrow().getStatus());
    }

    // ---------- helpers ----------

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, User user) {
        return request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt.createAccessToken(user.getId()));
    }

    private String deadlineJson(int plusDays) {
        return "{\"deadline\":\"" + LocalDateTime.now().plusDays(plusDays).withNano(0) + "\"}";
    }

    private String voteJson(Long candidateId, String preference) {
        return "{\"candidateId\":" + candidateId + ",\"preference\":\"" + preference + "\"}";
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

    private User saveUser(String prefix) {
        String suffix = UUID.randomUUID().toString();
        return userRepository.save(new User(prefix + "-" + suffix + "@example.com", prefix + "-" + suffix, "password"));
    }
}
