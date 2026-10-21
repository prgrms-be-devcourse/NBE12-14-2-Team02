package com.prgms.backend.domain.meeting;

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
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingInvitationExpiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingInvitationNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingInvitationServiceTest {

    private MeetingRepository meetingRepository;
    private MeetingInvitationRepository meetingInvitationRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private UserRepository userRepository;

    private MeetingInvitationService meetingInvitationService;

    @BeforeEach
    void setUp() {
        meetingRepository = mock(MeetingRepository.class);
        meetingInvitationRepository = mock(MeetingInvitationRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        userRepository = mock(UserRepository.class);

        meetingInvitationService = new MeetingInvitationService(
            meetingRepository,
            meetingInvitationRepository,
            meetingMemberRepository,
            userRepository
        );
    }

    @Test
    @DisplayName("모임장은 초대 코드를 생성할 수 있다")
    void createInvitation() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingInvitationRepository
                .save(any(MeetingInvitation.class))
        ).thenAnswer(invocation -> {

            MeetingInvitation invitation =
                invocation.getArgument(0);

            ReflectionTestUtils.setField(
                invitation,
                "id",
                100L
            );

            return invitation;
        });

        // when
        var response =
            meetingInvitationService.createInvitation(
                10L,
                1L
            );

        // then
        assertEquals(100L, response.id());
        assertEquals(10L, response.meetingId());

        assertNotNull(response.inviteCode());
        assertFalse(response.inviteCode().isBlank());

        assertNotNull(response.expiresAt());

        verify(meetingInvitationRepository)
            .save(any(MeetingInvitation.class));
    }

    @Test
    @DisplayName("존재하지 않는 모임에는 초대를 생성할 수 없다")
    void createInvitation_meetingNotFound() {

        // given
        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingInvitationService.createInvitation(
                10L,
                1L
            )
        );
    }

    @Test
    @DisplayName("모임장이 아니면 초대를 생성할 수 없다")
    void createInvitation_notHost() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingInvitationService.createInvitation(
                10L,
                2L
            )
        );
    }

    @Test
    @DisplayName("종료된 모임에는 초대를 생성할 수 없다")
    void createInvitation_completedMeeting() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        meeting.complete();

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingNotActiveException.class,
            () -> meetingInvitationService.createInvitation(
                10L,
                1L
            )
        );
    }

    @Test
    @DisplayName("초대를 통해 처음 모임에 참여할 수 있다")
    void joinMeeting_newMember() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        User user = createUser(
            2L,
            "user@test.com",
            "회원"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation = createInvitation(
            100L,
            meeting,
            "invite-code",
            LocalDateTime.now().plusDays(1)
        );

        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("invite-code"))
            .thenReturn(Optional.of(10L));

        when(
            meetingInvitationRepository
                .findByInviteCode("invite-code")
        ).thenReturn(Optional.of(invitation));

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(userRepository.findById(2L))
            .thenReturn(Optional.of(user));

        when(meetingMemberRepository
            .findByMeetingIdAndUserIdForUpdate(10L, 2L))
            .thenReturn(Optional.empty());

        when(
            meetingMemberRepository
                .saveAndFlush(any(MeetingMember.class))
        ).thenAnswer(invocation -> {

            MeetingMember member =
                invocation.getArgument(0);

            ReflectionTestUtils.setField(
                member,
                "id",
                200L
            );

            return member;
        });

        // when
        var response =
            meetingInvitationService.joinMeeting(
                "invite-code",
                2L
            );

        // then
        assertEquals(200L, response.id());
        assertEquals(10L, response.meetingId());
        assertEquals(2L, response.userId());
        assertEquals(MeetingMemberStatus.JOINED, response.status());

        // 가입 여부 판단에 사용할 조회가 호출됐는지 확인
        verify(meetingMemberRepository)
            .findByMeetingIdAndUserIdForUpdate(10L, 2L);

        // 신규 가입이므로 새 모임원을 저장
        verify(meetingMemberRepository)
            .saveAndFlush(any(MeetingMember.class));
    }

    @Test
    @DisplayName("새 모임원 저장 중 모임원 중복 키 오류가 발생하면 중복 참여 예외로 변환한다")
    void joinMeeting_duplicateKey() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        User user = createUser(
            2L,
            "user@test.com",
            "회원"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation = createInvitation(
            100L,
            meeting,
            "invite-code",
            LocalDateTime.now().plusDays(1)
        );

        // 초대 코드가 어느 모임에 속하는지 조회
        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("invite-code"))
            .thenReturn(Optional.of(10L));

        // 잠금을 걸면서 모임 조회
        when(meetingRepository
            .findByIdAndDeletedAtIsNullForUpdate(10L))
            .thenReturn(Optional.of(meeting));

        // 초대 정보 조회
        when(meetingInvitationRepository
            .findByInviteCode("invite-code"))
            .thenReturn(Optional.of(invitation));

        // 가입하려는 사용자 조회
        when(userRepository.findById(2L))
            .thenReturn(Optional.of(user));

        when(meetingMemberRepository
            .findByMeetingIdAndUserIdForUpdate(10L, 2L))
            .thenReturn(Optional.empty());

        // DB가 모임원 중복 제약 위반을 알린 상황을 준비
        ConstraintViolationException cause =
            mock(ConstraintViolationException.class);

        when(cause.getConstraintName())
            .thenReturn("uk_meeting_member_meeting_user");

        DataIntegrityViolationException dbException =
            new DataIntegrityViolationException("저장 실패", cause);

        when(meetingMemberRepository.saveAndFlush(any(MeetingMember.class)))
            .thenThrow(dbException);

        // 서비스가 DB 오류를 중복 참여 예외로 변환하는지 확인
        AlreadyMeetingMemberException exception = assertThrows(
            AlreadyMeetingMemberException.class,
            () -> meetingInvitationService.joinMeeting("invite-code", 2L)
        );

        assertEquals(409, exception.getCode());
    }

    @Test
    @DisplayName("탈퇴했던 회원은 초대를 통해 다시 가입할 수 있다")
    void joinMeeting_rejoin() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        User user = createUser(
            2L,
            "user@test.com",
            "회원"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation = createInvitation(
            100L,
            meeting,
            "invite-code",
            LocalDateTime.now().plusDays(1)
        );

        MeetingMember member =
            new MeetingMember(
                meeting,
                user
            );

        ReflectionTestUtils.setField(
            member,
            "id",
            200L
        );

        // 기존 회원이 탈퇴한 상태
        member.leave();

        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("invite-code"))
            .thenReturn(Optional.of(10L));

        when(
            meetingInvitationRepository
                .findByInviteCode("invite-code")
        ).thenReturn(Optional.of(invitation));

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(userRepository.findById(2L))
            .thenReturn(Optional.of(user));

        when(meetingMemberRepository
            .findByMeetingIdAndUserIdForUpdate(10L, 2L))
            .thenReturn(Optional.of(member));

        // when
        var response =
            meetingInvitationService.joinMeeting(
                "invite-code",
                2L
            );

        // then
        assertEquals(MeetingMemberStatus.JOINED, response.status());

        assertEquals(200L, response.id());
        assertEquals(10L, response.meetingId());
        assertEquals(2L, response.userId());

        // 재가입하면 기존 탈퇴 시각 해제
        assertNull(response.leftAt());

        // 기존 MeetingMember를 재사용하므로 새로 저장X
        verify(meetingMemberRepository, never())
            .saveAndFlush(any(MeetingMember.class));
    }

    @Test
    @DisplayName("이미 참여 중인 회원은 다시 가입할 수 없다")
    void joinMeeting_alreadyJoined() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        User user = createUser(
            2L,
            "user@test.com",
            "회원"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation = createInvitation(
            100L,
            meeting,
            "invite-code",
            LocalDateTime.now().plusDays(1)
        );

        MeetingMember member =
            new MeetingMember(
                meeting,
                user
            );

        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("invite-code"))
            .thenReturn(Optional.of(10L));

        when(
            meetingInvitationRepository
                .findByInviteCode("invite-code")
        ).thenReturn(Optional.of(invitation));

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(userRepository.findById(2L))
            .thenReturn(Optional.of(user));

        when(meetingMemberRepository
            .findByMeetingIdAndUserIdForUpdate(10L, 2L))
            .thenReturn(Optional.of(member));

        // when & then
        AlreadyMeetingMemberException exception = assertThrows(
            AlreadyMeetingMemberException.class,
            () -> meetingInvitationService.joinMeeting(
                "invite-code",
                2L
            )
        );

        assertEquals(409, exception.getCode());

        // 중복 참여자는 새로 저장X
        verify(meetingMemberRepository, never())
            .saveAndFlush(any(MeetingMember.class));
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드로는 모임에 가입할 수 없다")
    void joinMeeting_invitationNotFound() {

        // given
        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("invalid-code"))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingInvitationNotFoundException.class,
            () -> meetingInvitationService.joinMeeting(
                "invalid-code",
                2L
            )
        );
    }

    @Test
    @DisplayName("만료된 초대로는 모임에 가입할 수 없다")
    void joinMeeting_expiredInvitation() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation expiredInvitation =
            createInvitation(
                100L,
                meeting,
                "expired-code",
                LocalDateTime.now().minusDays(1)
            );

        when(meetingInvitationRepository
            .findMeetingIdByInviteCode("expired-code"))
            .thenReturn(Optional.of(10L));

        when(
            meetingInvitationRepository
                .findByInviteCode("expired-code")
        ).thenReturn(Optional.of(expiredInvitation));

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingInvitationExpiredException.class,
            () -> meetingInvitationService.joinMeeting(
                "expired-code",
                2L
            )
        );
    }

    @Test
    @DisplayName("모임장은 초대 코드 목록을 조회할 수 있다")
    void getInvitations() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation1 =
            createInvitation(
                100L,
                meeting,
                "invite-code-1",
                LocalDateTime.now().plusDays(1)
            );

        MeetingInvitation invitation2 =
            createInvitation(
                200L,
                meeting,
                "invite-code-2",
                LocalDateTime.now().plusDays(2)
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNull(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingInvitationRepository
                .findAllByMeetingId(10L)
        ).thenReturn(
            List.of(
                invitation1,
                invitation2
            )
        );

        // when
        var responses =
            meetingInvitationService.getInvitations(
                10L,
                1L
            );

        // then
        assertEquals(2, responses.size());

        assertEquals(
            "invite-code-1",
            responses.get(0).inviteCode()
        );

        assertEquals(
            "invite-code-2",
            responses.get(1).inviteCode()
        );

        assertEquals(
            10L,
            responses.get(0).meetingId()
        );
    }

    @Test
    @DisplayName("모임장이 아니면 초대 코드 목록을 조회할 수 없다")
    void getInvitations_notHost() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNull(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingInvitationService.getInvitations(
                10L,
                2L
            )
        );
    }

    @Test
    @DisplayName("유효한 초대 코드로 초대받은 모임 정보를 조회할 수 있다")
    void getInvitation() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation =
            createInvitation(
                100L,
                meeting,
                "invite-code",
                LocalDateTime.now().plusDays(1)
            );

        when(
            meetingInvitationRepository
                .findByInviteCode("invite-code")
        ).thenReturn(Optional.of(invitation));

        when(
            meetingMemberRepository
                .existsByMeetingIdAndUserIdAndStatus(
                    10L,
                    2L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(false);

        // when
        var response =
            meetingInvitationService.getInvitation(
                "invite-code",
                2L
            );

        // then
        assertEquals(
            "invite-code",
            response.inviteCode()
        );

        assertEquals(
            10L,
            response.meetingId()
        );

        assertEquals(
            "제주도 여행",
            response.meetingName()
        );

        assertFalse(
            response.alreadyJoined()
        );
    }

    @Test
    @DisplayName("이미 참여 중인 회원이 초대를 조회하면 참여 중으로 표시된다")
    void getInvitation_alreadyJoined() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation =
            createInvitation(
                100L,
                meeting,
                "invite-code",
                LocalDateTime.now().plusDays(1)
            );

        when(
            meetingInvitationRepository
                .findByInviteCode("invite-code")
        ).thenReturn(Optional.of(invitation));

        when(
            meetingMemberRepository
                .existsByMeetingIdAndUserIdAndStatus(
                    10L,
                    2L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(true);

        // when
        var response =
            meetingInvitationService.getInvitation(
                "invite-code",
                2L
            );

        // then
        assertEquals(
            10L,
            response.meetingId()
        );

        assertEquals(
            "제주도 여행",
            response.meetingName()
        );

        assertTrue(
            response.alreadyJoined()
        );
    }

    @Test
    @DisplayName("모임장은 초대 코드를 삭제할 수 있다")
    void deleteInvitation() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        MeetingInvitation invitation =
            createInvitation(
                100L,
                meeting,
                "invite-code",
                LocalDateTime.now().plusDays(1)
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingInvitationRepository
                .findById(100L)
        ).thenReturn(Optional.of(invitation));

        // when
        meetingInvitationService.deleteInvitation(
            10L,
            100L,
            1L
        );

        // then
        verify(meetingInvitationRepository)
            .delete(invitation);
    }

    @Test
    @DisplayName("모임장이 아니면 초대 코드를 삭제할 수 없다")
    void deleteInvitation_notHost() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingInvitationService.deleteInvitation(
                10L,
                100L,
                2L
            )
        );

        verify(
            meetingInvitationRepository,
            never()
        ).delete(any(MeetingInvitation.class));
    }

    @Test
    @DisplayName("종료된 모임의 초대 코드는 삭제할 수 없다")
    void deleteInvitation_completedMeeting() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        meeting.complete();

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingNotActiveException.class,
            () -> meetingInvitationService.deleteInvitation(
                10L,
                100L,
                1L
            )
        );

        verify(
            meetingInvitationRepository,
            never()
        ).delete(any(MeetingInvitation.class));
    }

    @Test
    @DisplayName("존재하지 않는 초대 코드는 삭제할 수 없다")
    void deleteInvitation_notFound() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingInvitationRepository
                .findById(100L)
        ).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingInvitationNotFoundException.class,
            () -> meetingInvitationService.deleteInvitation(
                10L,
                100L,
                1L
            )
        );

        verify(
            meetingInvitationRepository,
            never()
        ).delete(any(MeetingInvitation.class));
    }

    @Test
    @DisplayName("다른 모임의 초대 코드는 삭제할 수 없다")
    void deleteInvitation_otherMeeting() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "제주도 여행"
        );

        Meeting otherMeeting = createMeeting(
            20L,
            host,
            "부산 여행"
        );

        // 100번 초대는 10번 모임이 아니라
        // 20번 모임의 초대
        MeetingInvitation invitation =
            createInvitation(
                100L,
                otherMeeting,
                "other-invite-code",
                LocalDateTime.now().plusDays(1)
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForShare(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingInvitationRepository
                .findById(100L)
        ).thenReturn(Optional.of(invitation));

        // when & then
        assertThrows(
            MeetingInvitationNotFoundException.class,
            () -> meetingInvitationService.deleteInvitation(
                10L,
                100L,
                1L
            )
        );

        verify(
            meetingInvitationRepository,
            never()
        ).delete(any(MeetingInvitation.class));
    }

    private User createUser(
        Long id,
        String email,
        String nickname
    ) {
        User user = new User(
            email,
            nickname,
            "password"
        );

        ReflectionTestUtils.setField(
            user,
            "id",
            id
        );

        return user;
    }

    private Meeting createMeeting(
        Long id,
        User host,
        String name
    ) {
        Meeting meeting = new Meeting(
            host,
            name,
            "테스트 모임 설명"
        );

        ReflectionTestUtils.setField(
            meeting,
            "id",
            id
        );

        return meeting;
    }

    private MeetingInvitation createInvitation(
        Long id,
        Meeting meeting,
        String inviteCode,
        LocalDateTime expiresAt
    ) {
        MeetingInvitation invitation =
            new MeetingInvitation(
                meeting,
                inviteCode,
                expiresAt
            );

        ReflectionTestUtils.setField(
            invitation,
            "id",
            id
        );

        return invitation;
    }
}
