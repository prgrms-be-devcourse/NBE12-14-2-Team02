package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingInvitationService;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
}