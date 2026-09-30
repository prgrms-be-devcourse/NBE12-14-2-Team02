package com.prgms.backend.domain.user;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingMemberService;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.enums.UserStatus;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.domain.user.service.UserWithdrawService;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberHasUnsettledExpenseException;
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import com.prgms.backend.global.exception.custom.user.UserWithdrawActivateHostMeetingException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserWithdrawServiceTest {

    private UserRepository userRepository;
    private MeetingRepository meetingRepository;
    private MeetingMemberService meetingMemberService;
    private MeetingMemberRepository meetingMemberRepository;

    private UserWithdrawService userWithdrawService;

    private User user;
    private User otherHost;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        meetingRepository = mock(MeetingRepository.class);
        meetingMemberService = mock(MeetingMemberService.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);

        userWithdrawService = new UserWithdrawService(
            userRepository,
            meetingRepository,
            meetingMemberService,
            meetingMemberRepository
        );

        user = createUser(1L, "user@test.com", "탈퇴회원");
        user.updateRefreshToken("refresh-token");
        otherHost = createUser(2L, "host@test.com", "모임장");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));
    }

    @Test
    @DisplayName("참여 중인 모임을 모두 탈퇴하고, 호스트인 완료 모임을 삭제한 뒤 회원을 탈퇴 처리한다")
    void withdraw() {

        // given
        Meeting hostMeeting = createMeeting(10L, user);
        hostMeeting.complete();
        Meeting memberMeeting = createMeeting(20L, otherHost);

        givenNoActiveHostMeeting();

        when(
            meetingMemberRepository.findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                1L,
                MeetingMemberStatus.JOINED
            )
        ).thenReturn(List.of(
            new MeetingMember(hostMeeting, user),
            new MeetingMember(memberMeeting, user)
        ));

        when(meetingRepository.findByHostIdAndDeletedAtIsNull(1L))
            .thenReturn(List.of(hostMeeting));

        // when
        userWithdrawService.withdraw(1L);

        // then
        verify(meetingMemberService).leaveMeeting(10L, 1L);
        verify(meetingMemberService).leaveMeeting(20L, 1L);

        assertNotNull(hostMeeting.getDeletedAt());
        assertNull(memberMeeting.getDeletedAt());

        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
        assertNotNull(user.getDeletedAt());
        assertNull(user.getRefreshToken());
    }

    @Test
    @DisplayName("모임 탈퇴를 먼저 처리한 뒤 호스트 모임을 삭제한다")
    void withdrawLeavesBeforeSoftDelete() {

        // given
        Meeting hostMeeting = createMeeting(10L, user);
        hostMeeting.complete();

        givenNoActiveHostMeeting();

        when(
            meetingMemberRepository.findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                1L,
                MeetingMemberStatus.JOINED
            )
        ).thenReturn(List.of(new MeetingMember(hostMeeting, user)));

        when(meetingRepository.findByHostIdAndDeletedAtIsNull(1L))
            .thenReturn(List.of(hostMeeting));

        // when
        userWithdrawService.withdraw(1L);

        // then
        // soft delete가 먼저 되면 leaveMeeting에서 모임을 찾지 못한다
        InOrder inOrder = inOrder(meetingMemberService, meetingRepository);
        inOrder.verify(meetingMemberService).leaveMeeting(10L, 1L);
        inOrder.verify(meetingRepository).findByHostIdAndDeletedAtIsNull(1L);
    }

    @Test
    @DisplayName("참여 중인 모임이 없어도 회원 탈퇴할 수 있다")
    void withdrawWithoutMeeting() {

        // given
        givenNoActiveHostMeeting();

        when(
            meetingMemberRepository.findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                1L,
                MeetingMemberStatus.JOINED
            )
        ).thenReturn(List.of());

        when(meetingRepository.findByHostIdAndDeletedAtIsNull(1L))
            .thenReturn(List.of());

        // when
        userWithdrawService.withdraw(1L);

        // then
        verify(meetingMemberService, never()).leaveMeeting(anyLong(), anyLong());
        assertEquals(UserStatus.WITHDRAWN, user.getStatus());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 탈퇴할 수 없다")
    void withdrawUserNotFound() {

        // given
        when(userRepository.findById(99L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> userWithdrawService.withdraw(99L)
        );
    }

    @Test
    @DisplayName("진행 중인 모임의 모임장은 회원 탈퇴할 수 없다")
    void withdrawActiveHostMeeting() {

        // given
        when(
            meetingRepository.existsByHostIdAndStatusAndDeletedAtIsNull(
                1L,
                MeetingStatus.ACTIVE
            )
        ).thenReturn(true);

        // when & then
        assertThrows(
            UserWithdrawActivateHostMeetingException.class,
            () -> userWithdrawService.withdraw(1L)
        );

        verify(meetingMemberService, never()).leaveMeeting(anyLong(), anyLong());
        verify(meetingRepository, never()).findByHostIdAndDeletedAtIsNull(anyLong());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getDeletedAt());
    }

    @Test
    @DisplayName("미정산 지출이 있는 모임이 있으면 회원 탈퇴할 수 없다")
    void withdrawUnsettledExpense() {

        // given
        Meeting memberMeeting = createMeeting(20L, otherHost);

        givenNoActiveHostMeeting();

        when(
            meetingMemberRepository.findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                1L,
                MeetingMemberStatus.JOINED
            )
        ).thenReturn(List.of(new MeetingMember(memberMeeting, user)));

        when(meetingMemberService.leaveMeeting(20L, 1L))
            .thenThrow(new MeetingMemberHasUnsettledExpenseException(20L, 1L));

        // when & then
        assertThrows(
            MeetingMemberHasUnsettledExpenseException.class,
            () -> userWithdrawService.withdraw(1L)
        );

        verify(meetingRepository, never()).findByHostIdAndDeletedAtIsNull(anyLong());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNull(user.getDeletedAt());
        assertEquals("refresh-token", user.getRefreshToken());
    }

    private void givenNoActiveHostMeeting() {
        when(
            meetingRepository.existsByHostIdAndStatusAndDeletedAtIsNull(
                1L,
                MeetingStatus.ACTIVE
            )
        ).thenReturn(false);
    }

    private User createUser(Long id, String email, String nickname) {
        User user = new User(email, nickname, "password");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Meeting createMeeting(Long id, User host) {
        Meeting meeting = new Meeting(host, "모임", "모임 설명");
        ReflectionTestUtils.setField(meeting, "id", id);
        return meeting;
    }
}
