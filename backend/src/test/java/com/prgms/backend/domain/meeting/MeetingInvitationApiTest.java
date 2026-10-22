package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.meeting.controller.MeetingInvitationController;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingInvitationService;
import com.prgms.backend.domain.user.entity.SecurityUser;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.GlobalExceptionHandler;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MeetingInvitationApiTest {

    private MeetingMemberRepository members;
    private MockMvc mvc;
    private Meeting meeting;
    private User user;

    @BeforeEach
    void setUp() {
        MeetingRepository meetings = mock(MeetingRepository.class);
        MeetingInvitationRepository invitations = mock(MeetingInvitationRepository.class);
        members = mock(MeetingMemberRepository.class);
        UserRepository users = mock(UserRepository.class);
        MeetingInvitationService service = new MeetingInvitationService(meetings, invitations, members, users);
        mvc = MockMvcBuilders.standaloneSetup(new MeetingInvitationController(service))
            .setControllerAdvice(new GlobalExceptionHandler())
            .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
            .build();

        user = new User("member@test.com", "회원", "password");
        ReflectionTestUtils.setField(user, "id", 2L);
        meeting = new Meeting(user, "초대 테스트", "");
        ReflectionTestUtils.setField(meeting, "id", 10L);
        MeetingInvitation invitation = new MeetingInvitation(
            meeting, "invite-code", LocalDateTime.now().plusDays(1));
        when(invitations.findMeetingIdByInviteCode("invite-code")).thenReturn(Optional.of(10L));
        when(meetings.findByIdAndDeletedAtIsNullForUpdate(10L)).thenReturn(Optional.of(meeting));
        when(invitations.findByInviteCode("invite-code")).thenReturn(Optional.of(invitation));
        when(users.findById(2L)).thenReturn(Optional.of(user));
        when(members.findByMeetingIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.empty());
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(new SecurityUser(2L), null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("조회에서 이미 가입한 모임원을 발견하면 HTTP 409로 응답한다")
    void alreadyJoinedReturnsConflict() throws Exception {
        when(members.findByMeetingIdAndUserIdForUpdate(10L, 2L))
            .thenReturn(Optional.of(new MeetingMember(meeting, user)));

        mvc.perform(post("/api/invitations/invite-code/join"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.success").value(false));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "uk_meeting_member_meeting_user",
        "meeting_members.uk_meeting_member_meeting_user"
    })
    @DisplayName("저장 중 모임원 중복 UNIQUE 위반이 발생하면 HTTP 409로 응답한다")
    void duplicateMembershipReturnsConflict(String constraintName) throws Exception {
        failOnSave(constraintViolation(constraintName));

        mvc.perform(post("/api/invitations/invite-code/join"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value(409))
            .andExpect(jsonPath("$.message").value(
                "이미 모임에 참여 중인 사용자입니다. meetingId = 10, userId = 2"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {
        "uk_users_email", "fk_meeting_member_user",
        "uk_meeting_member_meeting_user_other",
        "other_table.uk_meeting_member_meeting_user"
    })
    @DisplayName("다른 UNIQUE·외래 키·식별 불가 오류는 중복 참여 409로 변환하지 않는다")
    void unrelatedConstraintReturnsServerError(String constraintName) throws Exception {
        failOnSave(constraintViolation(constraintName));

        mvc.perform(post("/api/invitations/invite-code/join"))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("메시지에 제약 이름만 있는 일반 무결성 오류는 409로 변환하지 않는다")
    void messageAloneDoesNotIdentifyDuplicateMembership() throws Exception {
        failOnSave(new DataIntegrityViolationException("uk_meeting_member_meeting_user"));

        mvc.perform(post("/api/invitations/invite-code/join"))
            .andExpect(status().isInternalServerError());
    }

    private void failOnSave(DataIntegrityViolationException exception) {
        when(members.saveAndFlush(any(MeetingMember.class))).thenThrow(exception);
    }

    private DataIntegrityViolationException constraintViolation(String constraintName) {
        return new DataIntegrityViolationException("저장 실패",
            new ConstraintViolationException("제약 위반", new SQLException("제약 위반"), constraintName));
    }
}
