package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.expense.entity.Expense;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.request.MeetingUpdateRequest;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.settlement.entity.Settlement;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// JWT 인증부터 Controller, Service, Repository, MySQL까지 함께 검증한다.
// 각 테스트의 데이터는 @Transactional에 의해 테스트 종료 후 롤백된다.
@SpringBootTest(properties = {
    "spring.config.import=",
    "spring.flyway.enabled=false",
    "spring.jpa.hibernate.ddl-auto=create",
    "spring.jpa.show-sql=false",
    "logging.level.org.hibernate.SQL=OFF",
    "custom.jwt.access-secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
    "custom.jwt.refresh-secret=abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
    "custom.jwt.access-token-validity-seconds=1800",
    "custom.jwt.refresh-token-validity-seconds=1209600"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class MeetingIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0")
        .withDatabaseName("meeting_api_test");

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private JwtTokenProvider tokens;
    @Autowired private UserRepository users;
    @Autowired private MeetingRepository meetings;
    @Autowired private MeetingMemberRepository members;
    @Autowired private MeetingInvitationRepository invitations;
    @Autowired private SchedulePollRepository schedulePolls;
    @Autowired private ContentPollRepository contentPolls;
    @Autowired private ExpenseRepository expenses;
    @Autowired private SettlementRepository settlements;
    @Autowired private EntityManager entityManager;

    private User host;
    private User member;
    private User outsider;
    private Meeting meeting;
    private MeetingMember hostMember;
    private String url;

    @BeforeEach
    void setUp() {
        // 공통 준비 데이터만 저장소로 생성하고, 검증할 동작은 API로 호출한다.
        host = users.save(new User("host@test.com", "모임장", "password"));
        member = users.save(new User("member@test.com", "회원", "password"));
        outsider = users.save(new User("outsider@test.com", "외부인", "password"));
        meeting = meetings.save(new Meeting(host, "제주도 여행", "통합 테스트 모임"));
        hostMember = members.save(new MeetingMember(meeting, host));
        url = "/api/meetings/" + meeting.getId();
        flushAndClear();
    }

    // 모임 API: 생성, 조회, 수정, 종료

    @Test
    @DisplayName("모임 생성 API는 201을 반환하고 생성자를 JOINED 모임원으로 저장한다")
    void createMeeting_registersHost() throws Exception {
        MvcResult result = mvc.perform(post("/api/meetings")
                .header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new MeetingCreateRequest("부산 여행", "새 모임"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.data.hostId").value(host.getId()))
            .andExpect(jsonPath("$.data.status").value("ACTIVE"))
            .andExpect(jsonPath("$.data.participantCount").value(1))
            .andReturn();

        Long meetingId = data(result).path("id").asLong();
        flushAndClear();
        assertEquals("부산 여행", meetings.findById(meetingId).orElseThrow().getName());
        assertEquals(MeetingMemberStatus.JOINED,
            members.findByMeetingIdAndUserId(meetingId, host.getId()).orElseThrow().getStatus());
        assertEquals(1L, members.countByMeetingIdAndStatus(meetingId, MeetingMemberStatus.JOINED));
    }

    @Test
    @DisplayName("공백 모임 이름은 400으로 거절하고 모임을 저장하지 않는다")
    void createMeeting_blankName() throws Exception {
        long before = meetings.count();
        assertError(mvc.perform(post("/api/meetings")
            .header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingCreateRequest("   ", "설명")))), 400);
        assertEquals(before, meetings.count());
    }

    @Test
    @DisplayName("100자를 초과한 모임 이름은 400으로 거절한다")
    void createMeeting_nameTooLong() throws Exception {
        assertError(mvc.perform(post("/api/meetings")
            .header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingCreateRequest("가".repeat(101), "설명")))), 400);
    }

    @Test
    @DisplayName("500자를 초과한 모임 설명은 400으로 거절한다")
    void createMeeting_descriptionTooLong() throws Exception {
        assertError(mvc.perform(post("/api/meetings")
            .header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingCreateRequest("모임", "가".repeat(501))))), 400);
    }

    @Test
    @DisplayName("로그인하지 않은 요청은 401로 거절한다")
    void unauthenticatedRequest() throws Exception {
        assertError(mvc.perform(get("/api/meetings")), 401);
    }

    @Test
    @DisplayName("유효하지 않은 JWT 요청은 401로 거절한다")
    void invalidToken() throws Exception {
        assertError(mvc.perform(get("/api/meetings").header(AUTHORIZATION, "Bearer invalid-token")), 401);
    }

    @Test
    @DisplayName("JOINED 모임원은 모임 상세 정보를 조회할 수 있다")
    void getMeeting_joinedMember() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        mvc.perform(get(url).header(AUTHORIZATION, bearer(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(meeting.getId()))
            .andExpect(jsonPath("$.data.name").value("제주도 여행"))
            .andExpect(jsonPath("$.data.participantCount").value(2));
    }

    @Test
    @DisplayName("외부인은 모임 상세 정보를 조회할 수 없다")
    void getMeeting_outsider() throws Exception {
        assertError(mvc.perform(get(url).header(AUTHORIZATION, bearer(outsider))), 403);
    }

    @Test
    @DisplayName("존재하지 않는 모임은 404로 응답한다")
    void getMeeting_notFound() throws Exception {
        assertError(mvc.perform(get("/api/meetings/" + Long.MAX_VALUE)
            .header(AUTHORIZATION, bearer(host))), 404);
    }

    @Test
    @DisplayName("삭제된 모임은 상세 조회와 내 모임 목록에서 제외된다")
    void deletedMeeting_isHidden() throws Exception {
        meetings.findById(meeting.getId()).orElseThrow().softDelete();
        flushAndClear();
        assertError(mvc.perform(get(url).header(AUTHORIZATION, bearer(host))), 404);
        mvc.perform(get("/api/meetings").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("내 모임 목록은 참여 중인 모임과 참가자 수를 반환한다")
    void getMyMeetings_joinedOnly() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        mvc.perform(get("/api/meetings").header(AUTHORIZATION, bearer(member)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(meeting.getId()))
            .andExpect(jsonPath("$.data[0].participantCount").value(2));
        mvc.perform(get("/api/meetings").header(AUTHORIZATION, bearer(outsider)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("모임장은 모임 정보를 수정할 수 있고 DB에도 반영된다")
    void updateMeeting_host() throws Exception {
        mvc.perform(put(url).header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(new MeetingUpdateRequest("변경된 이름", "변경된 설명"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.name").value("변경된 이름"));
        flushAndClear();
        Meeting saved = meetings.findById(meeting.getId()).orElseThrow();
        assertEquals("변경된 이름", saved.getName());
        assertEquals("변경된 설명", saved.getDescription());
    }

    @Test
    @DisplayName("일반 모임원은 모임을 수정할 수 없고 기존 정보가 유지된다")
    void updateMeeting_notHost() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        assertError(mvc.perform(put(url).header(AUTHORIZATION, bearer(member)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingUpdateRequest("변경 시도", "설명")))), 403);
        flushAndClear();
        assertEquals("제주도 여행", meetings.findById(meeting.getId()).orElseThrow().getName());
    }

    @Test
    @DisplayName("모임 수정 요청에도 이름 검증을 적용한다")
    void updateMeeting_blankName() throws Exception {
        assertError(mvc.perform(put(url).header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingUpdateRequest("", "설명")))), 400);
    }

    @Test
    @DisplayName("지출과 열린 투표가 없으면 모임을 종료하고 종료된 모임도 조회할 수 있다")
    void completeMeeting_withoutExpense() throws Exception {
        completeMeeting();
        flushAndClear();
        assertEquals(MeetingStatus.COMPLETED, meetings.findById(meeting.getId()).orElseThrow().getStatus());
        mvc.perform(get(url).header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("COMPLETED"));
        mvc.perform(get("/api/meetings").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].status").value("COMPLETED"));
    }

    @Test
    @DisplayName("외부인은 모임을 종료할 수 없고 ACTIVE 상태가 유지된다")
    void completeMeeting_notHost() throws Exception {
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(outsider))), 403);
        assertMeetingActive();
    }

    @Test
    @DisplayName("열린 일정 투표가 있으면 모임 종료를 409로 거절한다")
    void completeMeeting_openSchedulePoll() throws Exception {
        schedulePolls.saveAndFlush(SchedulePoll.create(
            meetings.findById(meeting.getId()).orElseThrow(), LocalDateTime.now().plusDays(7)));
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host))), 409);
        assertMeetingActive();
    }

    @Test
    @DisplayName("열린 콘텐츠 투표가 있으면 모임 종료를 409로 거절한다")
    void completeMeeting_openContentPoll() throws Exception {
        contentPolls.saveAndFlush(new ContentPoll(meeting.getId(), LocalDateTime.now().plusDays(7)));
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host))), 409);
        assertMeetingActive();
    }

    @Test
    @DisplayName("지출이 있는데 정산이 없으면 모임을 종료할 수 없다")
    void completeMeeting_withoutSettlement() throws Exception {
        addExpense(hostMember);
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host))), 409);
        assertMeetingActive();
    }

    @Test
    @DisplayName("지출이 있고 정산이 OPEN이면 모임을 종료할 수 없다")
    void completeMeeting_openSettlement() throws Exception {
        addExpense(hostMember);
        settlements.saveAndFlush(new Settlement(meeting.getId()));
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host))), 409);
        assertMeetingActive();
    }

    @Test
    @DisplayName("지출이 있어도 정산이 CLOSED이면 모임을 종료할 수 있다")
    void completeMeeting_closedSettlement() throws Exception {
        addExpense(hostMember);
        closeSettlement();
        completeMeeting();
        flushAndClear();
        assertEquals(MeetingStatus.COMPLETED, meetings.findById(meeting.getId()).orElseThrow().getStatus());
    }

    @Test
    @DisplayName("종료된 모임은 수정하거나 다시 종료할 수 없다")
    void completedMeeting_rejectsChanges() throws Exception {
        completeMeeting();
        assertError(mvc.perform(put(url).header(AUTHORIZATION, bearer(host)).contentType(APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new MeetingUpdateRequest("수정 시도", "설명")))), 409);
        assertError(mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host))), 409);
        flushAndClear();
        assertEquals("제주도 여행", meetings.findById(meeting.getId()).orElseThrow().getName());
    }

    // 초대 API: 생성, 조회, 삭제, 가입

    @Test
    @DisplayName("모임장은 초대를 생성·목록 조회할 수 있고 외부인은 유효한 초대 정보를 조회할 수 있다")
    void invitation_createAndRead() throws Exception {
        JsonNode invitation = createInvitation();
        String code = invitation.path("inviteCode").asString();
        flushAndClear();
        assertEquals(meeting.getId(), invitations.findByInviteCode(code).orElseThrow().getMeeting().getId());
        mvc.perform(get(url + "/invitations").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].inviteCode").value(code));
        mvc.perform(get("/api/invitations/" + code).header(AUTHORIZATION, bearer(outsider)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.meetingId").value(meeting.getId()))
            .andExpect(jsonPath("$.data.alreadyJoined").value(false))
            .andExpect(jsonPath("$.data.participantCount").value(1));
    }

    @Test
    @DisplayName("일반 모임원은 초대를 생성할 수 없다")
    void createInvitation_notHost() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        long before = invitations.count();
        assertError(mvc.perform(post(url + "/invitations").header(AUTHORIZATION, bearer(member))), 403);
        assertEquals(before, invitations.count());
    }

    @Test
    @DisplayName("외부인은 초대 코드 목록을 조회할 수 없다")
    void getInvitations_notHost() throws Exception {
        assertError(mvc.perform(get(url + "/invitations").header(AUTHORIZATION, bearer(outsider))), 403);
    }

    @Test
    @DisplayName("모임장이 초대를 삭제하면 조회와 가입이 모두 404로 거절된다")
    void deleteInvitation_revokesCode() throws Exception {
        JsonNode invitation = createInvitation();
        String code = invitation.path("inviteCode").asString();
        Long invitationId = invitation.path("id").asLong();
        mvc.perform(delete(url + "/invitations/" + invitationId).header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200));
        flushAndClear();
        assertFalse(invitations.existsById(invitationId));
        assertError(mvc.perform(get("/api/invitations/" + code).header(AUTHORIZATION, bearer(member))), 404);
        assertError(mvc.perform(post("/api/invitations/" + code + "/join")
            .header(AUTHORIZATION, bearer(member))), 404);
        assertTrue(members.findByMeetingIdAndUserId(meeting.getId(), member.getId()).isEmpty());
    }

    @Test
    @DisplayName("모임장이 아니면 초대를 삭제할 수 없고 초대가 유지된다")
    void deleteInvitation_notHost() throws Exception {
        JsonNode invitation = createInvitation();
        Long invitationId = invitation.path("id").asLong();
        assertError(mvc.perform(delete(url + "/invitations/" + invitationId)
            .header(AUTHORIZATION, bearer(outsider))), 403);
        assertTrue(invitations.existsById(invitationId));
    }

    @Test
    @DisplayName("다른 모임 소속 초대는 404로 거절하고 삭제하지 않는다")
    void deleteInvitation_otherMeeting() throws Exception {
        Meeting otherMeeting = meetings.saveAndFlush(new Meeting(host, "다른 모임", ""));
        MeetingInvitation otherInvitation = invitations.saveAndFlush(new MeetingInvitation(
            otherMeeting, "other-meeting-code", LocalDateTime.now().plusDays(7)));
        assertError(mvc.perform(delete(url + "/invitations/" + otherInvitation.getId())
            .header(AUTHORIZATION, bearer(host))), 404);
        assertTrue(invitations.existsById(otherInvitation.getId()));
    }

    @Test
    @DisplayName("존재하지 않는 초대의 조회와 가입은 404로 응답한다")
    void invitation_notFound() throws Exception {
        assertError(mvc.perform(get("/api/invitations/missing-code")
            .header(AUTHORIZATION, bearer(member))), 404);
        assertError(mvc.perform(post("/api/invitations/missing-code/join")
            .header(AUTHORIZATION, bearer(member))), 404);
    }

    @Test
    @DisplayName("만료된 초대의 조회와 가입은 410으로 거절하고 모임원을 추가하지 않는다")
    void invitation_expired() throws Exception {
        invitations.saveAndFlush(new MeetingInvitation(
            meetings.findById(meeting.getId()).orElseThrow(), "expired-code", LocalDateTime.now().minusDays(1)));
        assertError(mvc.perform(get("/api/invitations/expired-code")
            .header(AUTHORIZATION, bearer(member))), 410);
        assertError(mvc.perform(post("/api/invitations/expired-code/join")
            .header(AUTHORIZATION, bearer(member))), 410);
        assertTrue(members.findByMeetingIdAndUserId(meeting.getId(), member.getId()).isEmpty());
    }

    @Test
    @DisplayName("초대 가입 API는 사용자·모임·JOINED 상태를 반환하고 실제 모임원을 저장한다")
    void joinMeeting_withInvitation() throws Exception {
        String code = createInvitation().path("inviteCode").asString();
        JsonNode response = join(member, code);
        flushAndClear();
        MeetingMember saved = memberRecord(member);
        assertEquals(saved.getId().longValue(), response.path("id").asLong());
        assertEquals(meeting.getId().longValue(), response.path("meetingId").asLong());
        assertEquals(member.getId().longValue(), response.path("userId").asLong());
        assertEquals("JOINED", response.path("status").asString());
        assertEquals(MeetingMemberStatus.JOINED, saved.getStatus());
        assertNotNull(saved.getJoinedAt());
        assertNull(saved.getLeftAt());
        assertEquals(2L, joinedCount());
        mvc.perform(get("/api/invitations/" + code).header(AUTHORIZATION, bearer(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.alreadyJoined").value(true))
            .andExpect(jsonPath("$.data.participantCount").value(2));
    }

    @Test
    @DisplayName("이미 가입한 사용자의 중복 요청은 HTTP와 본문 모두 409이고 모임원 행은 유지된다")
    void joinMeeting_duplicateReturns409() throws Exception {
        String code = createInvitation().path("inviteCode").asString();
        Long originalId = join(member, code).path("id").asLong();
        assertError(mvc.perform(post("/api/invitations/" + code + "/join")
            .header(AUTHORIZATION, bearer(member))), 409);
        flushAndClear();
        assertEquals(originalId, memberRecord(member).getId());
        assertEquals(2L, members.count());
    }

    @Test
    @DisplayName("같은 모임의 다른 초대 코드로도 중복 가입할 수 없다")
    void joinMeeting_duplicateWithDifferentCode() throws Exception {
        String firstCode = createInvitation().path("inviteCode").asString();
        String otherCode = createInvitation().path("inviteCode").asString();
        Long originalId = join(member, firstCode).path("id").asLong();
        assertError(mvc.perform(post("/api/invitations/" + otherCode + "/join")
            .header(AUTHORIZATION, bearer(member))), 409);
        flushAndClear();
        assertEquals(originalId, memberRecord(member).getId());
        assertEquals(2L, members.count());
    }

    @Test
    @DisplayName("서로 다른 사용자는 같은 초대 코드로 각각 가입할 수 있다")
    void joinMeeting_differentUsers() throws Exception {
        String code = createInvitation().path("inviteCode").asString();
        join(member, code);
        join(outsider, code);
        flushAndClear();
        assertEquals(MeetingMemberStatus.JOINED, memberRecord(member).getStatus());
        assertEquals(MeetingMemberStatus.JOINED, memberRecord(outsider).getStatus());
        assertEquals(3L, joinedCount());
    }

    @Test
    @DisplayName("종료된 모임은 초대 생성·초대 조회·가입·초대 삭제를 409로 거절한다")
    void completedMeeting_rejectsInvitations() throws Exception {
        JsonNode invitation = createInvitation();
        String code = invitation.path("inviteCode").asString();
        completeMeeting();
        assertError(mvc.perform(post(url + "/invitations").header(AUTHORIZATION, bearer(host))), 409);
        assertError(mvc.perform(get("/api/invitations/" + code).header(AUTHORIZATION, bearer(member))), 409);
        assertError(mvc.perform(post("/api/invitations/" + code + "/join")
            .header(AUTHORIZATION, bearer(member))), 409);
        assertError(mvc.perform(delete(url + "/invitations/" + invitation.path("id").asLong())
            .header(AUTHORIZATION, bearer(host))), 409);
        assertTrue(members.findByMeetingIdAndUserId(meeting.getId(), member.getId()).isEmpty());
        assertTrue(invitations.existsById(invitation.path("id").asLong()));
    }

    @Test
    @DisplayName("삭제된 모임의 초대로는 가입할 수 없다")
    void deletedMeeting_rejectsJoin() throws Exception {
        String code = createInvitation().path("inviteCode").asString();
        meetings.findById(meeting.getId()).orElseThrow().softDelete();
        flushAndClear();
        assertError(mvc.perform(post("/api/invitations/" + code + "/join")
            .header(AUTHORIZATION, bearer(member))), 404);
        assertTrue(members.findByMeetingIdAndUserId(meeting.getId(), member.getId()).isEmpty());
    }

    // 모임원 API: 목록, 탈퇴, 재가입

    @Test
    @DisplayName("모임원 목록은 JOINED 회원만 반환하고 외부인은 조회할 수 없다")
    void getMeetingMembers_accessAndStatus() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        mvc.perform(get(url + "/members").header(AUTHORIZATION, bearer(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2));
        assertError(mvc.perform(get(url + "/members").header(AUTHORIZATION, bearer(outsider))), 403);
        leave(member);
        mvc.perform(get(url + "/members").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].userId").value(host.getId()));
    }

    @Test
    @DisplayName("일반 모임원이 탈퇴하면 LEFT와 탈퇴 시각이 저장되고 모임 조회 권한을 잃는다")
    void leaveMeeting_updatesStateAndAccess() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        leave(member);
        flushAndClear();
        MeetingMember left = memberRecord(member);
        assertEquals(MeetingMemberStatus.LEFT, left.getStatus());
        assertNotNull(left.getLeftAt());
        assertEquals(1L, joinedCount());
        assertError(mvc.perform(get(url).header(AUTHORIZATION, bearer(member))), 403);
        mvc.perform(get("/api/meetings").header(AUTHORIZATION, bearer(member)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("탈퇴 후 재가입 API는 기존 모임원 ID를 재사용하고 탈퇴 시각을 해제한다")
    void rejoinMeeting_reusesMemberId() throws Exception {
        String code = createInvitation().path("inviteCode").asString();
        Long originalId = join(member, code).path("id").asLong();
        leave(member);
        flushAndClear();
        assertEquals(MeetingMemberStatus.LEFT, memberRecord(member).getStatus());
        assertNotNull(memberRecord(member).getLeftAt());
        JsonNode response = join(member, code);
        flushAndClear();
        MeetingMember rejoined = memberRecord(member);
        assertEquals(originalId.longValue(), response.path("id").asLong());
        assertEquals(originalId, rejoined.getId());
        assertEquals("JOINED", response.path("status").asString());
        assertTrue(response.path("leftAt").isNull());
        assertEquals(MeetingMemberStatus.JOINED, rejoined.getStatus());
        assertNull(rejoined.getLeftAt());
        assertNotNull(rejoined.getJoinedAt());
        assertEquals(2L, members.count());
        assertEquals(2L, joinedCount());
    }

    @Test
    @DisplayName("진행 중인 모임의 모임장은 탈퇴할 수 없다")
    void leaveMeeting_activeHost() throws Exception {
        assertError(mvc.perform(delete(url + "/members/me").header(AUTHORIZATION, bearer(host))), 409);
        flushAndClear();
        assertEquals(MeetingMemberStatus.JOINED, memberRecord(host).getStatus());
        assertNull(memberRecord(host).getLeftAt());
    }

    @Test
    @DisplayName("종료된 모임의 모임장은 탈퇴할 수 있다")
    void leaveMeeting_completedHost() throws Exception {
        completeMeeting();
        leave(host);
        flushAndClear();
        assertEquals(MeetingMemberStatus.LEFT, memberRecord(host).getStatus());
        assertNotNull(memberRecord(host).getLeftAt());
    }

    @Test
    @DisplayName("가입한 적 없는 사용자의 탈퇴 요청은 404로 거절한다")
    void leaveMeeting_notMember() throws Exception {
        assertError(mvc.perform(delete(url + "/members/me").header(AUTHORIZATION, bearer(outsider))), 404);
        assertEquals(1L, joinedCount());
    }

    @Test
    @DisplayName("이미 탈퇴한 사용자는 다시 탈퇴할 수 없고 최초 탈퇴 시각이 유지된다")
    void leaveMeeting_alreadyLeft() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        leave(member);
        flushAndClear();
        LocalDateTime originalLeftAt = memberRecord(member).getLeftAt();
        assertError(mvc.perform(delete(url + "/members/me").header(AUTHORIZATION, bearer(member))), 409);
        flushAndClear();
        assertEquals(originalLeftAt, memberRecord(member).getLeftAt());
        assertEquals(MeetingMemberStatus.LEFT, memberRecord(member).getStatus());
    }

    @Test
    @DisplayName("미정산 지출에 포함된 모임원은 탈퇴할 수 없고 가입 상태가 유지된다")
    void leaveMeeting_unsettledExpense() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        addExpense(memberRecord(member));
        assertError(mvc.perform(delete(url + "/members/me").header(AUTHORIZATION, bearer(member))), 409);
        flushAndClear();
        assertEquals(MeetingMemberStatus.JOINED, memberRecord(member).getStatus());
        assertNull(memberRecord(member).getLeftAt());
    }

    @Test
    @DisplayName("정산이 CLOSED이면 지출 참여자도 탈퇴할 수 있다")
    void leaveMeeting_closedSettlement() throws Exception {
        join(member, createInvitation().path("inviteCode").asString());
        addExpense(memberRecord(member));
        closeSettlement();
        leave(member);
        flushAndClear();
        assertEquals(MeetingMemberStatus.LEFT, memberRecord(member).getStatus());
        assertNotNull(memberRecord(member).getLeftAt());
    }

    // 반복되는 JWT 발급, 성공 요청, DB 재조회 준비만 보조 메서드로 모은다.

    private String bearer(User user) {
        return "Bearer " + tokens.createAccessToken(user.getId());
    }

    private JsonNode data(MvcResult result) {
        return jsonMapper.readTree(result.getResponse().getContentAsByteArray()).path("data");
    }

    private JsonNode createInvitation() throws Exception {
        MvcResult result = mvc.perform(post(url + "/invitations").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value(201))
            .andExpect(jsonPath("$.data.inviteCode").isNotEmpty()).andReturn();
        return data(result);
    }

    private JsonNode join(User user, String code) throws Exception {
        MvcResult result = mvc.perform(post("/api/invitations/" + code + "/join")
                .header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.code").value(200)).andReturn();
        return data(result);
    }

    private void leave(User user) throws Exception {
        mvc.perform(delete(url + "/members/me").header(AUTHORIZATION, bearer(user)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("LEFT"));
    }

    private void completeMeeting() throws Exception {
        mvc.perform(patch(url + "/complete").header(AUTHORIZATION, bearer(host)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    private void assertError(ResultActions result, int code) throws Exception {
        result.andExpect(status().is(code))
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.code").value(code));
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private MeetingMember memberRecord(User user) {
        return members.findByMeetingIdAndUserId(meeting.getId(), user.getId()).orElseThrow();
    }

    private long joinedCount() {
        return members.countByMeetingIdAndStatus(meeting.getId(), MeetingMemberStatus.JOINED);
    }

    private void assertMeetingActive() {
        flushAndClear();
        assertEquals(MeetingStatus.ACTIVE, meetings.findById(meeting.getId()).orElseThrow().getStatus());
    }

    private void addExpense(MeetingMember payer) {
        expenses.saveAndFlush(new Expense(meeting.getId(), payer.getId(), "식사", 1000L, null,
            Set.of(payer.getId())));
    }

    private void closeSettlement() {
        Settlement settlement = new Settlement(meeting.getId());
        settlement.close(hostMember.getId());
        settlements.saveAndFlush(settlement);
    }
}
