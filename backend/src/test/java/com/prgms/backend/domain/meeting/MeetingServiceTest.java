package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.request.MeetingUpdateRequest;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.enums.MeetingStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingService;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;

import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingPollNotCompletedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingSettlementNotCompletedException;
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeetingServiceTest {

    private MeetingRepository meetingRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private UserRepository userRepository;
    private SettlementRepository settlementRepository;
    private ExpenseRepository expenseRepository;
    private SchedulePollRepository schedulePollRepository;
    private ContentPollRepository contentPollRepository;

    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        meetingRepository = mock(MeetingRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        userRepository = mock(UserRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        expenseRepository = mock(ExpenseRepository.class);

        schedulePollRepository = mock(SchedulePollRepository.class);
        contentPollRepository = mock(ContentPollRepository.class);

        meetingService = new MeetingService(
            meetingRepository,
            meetingMemberRepository,
            userRepository,
            settlementRepository,
            expenseRepository,
            schedulePollRepository,
            contentPollRepository
        );
    }

    @Test
    @DisplayName("모임을 생성하면 생성자는 모임장으로 등록된다")
    void createMeeting() {

        // given
        User host = new User(
            "host@test.com",
            "모임장",
            "password"
        );

        ReflectionTestUtils.setField(
            host,
            "id",
            1L
        );

        MeetingCreateRequest request =
            new MeetingCreateRequest(
                "제주도 여행",
                "제주도 여행 모임"
            );

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(host));

        when(meetingRepository.save(any(Meeting.class)))
            .thenAnswer(invocation -> {

                Meeting meeting =
                    invocation.getArgument(0);

                ReflectionTestUtils.setField(
                    meeting,
                    "id",
                    10L
                );

                return meeting;
            });

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(1L);

        // when
        var response =
            meetingService.createMeeting(
                1L,
                request
            );

        // then
        assertEquals(10L, response.id());
        assertEquals(1L, response.hostId());
        assertEquals("제주도 여행", response.name());
        assertEquals(1L, response.participantCount());

        verify(meetingRepository)
            .save(any(Meeting.class));

        verify(meetingMemberRepository)
            .save(any(MeetingMember.class));
    }

    @Test
    @DisplayName("참여 중인 회원은 모임 상세 정보를 조회할 수 있다")
    void getMeeting() {

        // given
        User host = new User(
            "host@test.com",
            "모임장",
            "password"
        );

        ReflectionTestUtils.setField(
            host,
            "id",
            1L
        );

        Meeting meeting =
            new Meeting(
                host,
                "제주도 여행",
                "제주도 여행 모임"
            );

        ReflectionTestUtils.setField(
            meeting,
            "id",
            10L
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNull(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingMemberRepository
                .existsByMeetingIdAndUserIdAndStatus(
                    10L,
                    2L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(true);

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(3L);

        // when
        var response =
            meetingService.getMeeting(
                10L,
                2L
            );

        // then
        assertEquals(10L, response.id());
        assertEquals("제주도 여행", response.name());
        assertEquals(3L, response.participantCount());
    }

    @Test
    @DisplayName("참여 중인 회원이 아니면 모임 상세 정보를 조회할 수 없다")
    void getMeeting_notMember() {

        // given
        User host = new User(
            "host@test.com",
            "모임장",
            "password"
        );

        ReflectionTestUtils.setField(
            host,
            "id",
            1L
        );

        Meeting meeting =
            new Meeting(
                host,
                "제주도 여행",
                "제주도 여행 모임"
            );

        ReflectionTestUtils.setField(
            meeting,
            "id",
            10L
        );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNull(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingMemberRepository
                .existsByMeetingIdAndUserIdAndStatus(
                    10L,
                    2L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(false);

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingService.getMeeting(
                10L,
                2L
            )
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원은 모임을 생성할 수 없다")
    void createMeeting_userNotFound() {

        // given
        MeetingCreateRequest request =
            new MeetingCreateRequest(
                "제주도 여행",
                "제주도 여행 모임"
            );

        when(userRepository.findById(1L))
            .thenReturn(Optional.empty());

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> meetingService.createMeeting(
                1L,
                request
            )
        );
    }


    @Test
    @DisplayName("존재하지 않는 모임은 상세 조회할 수 없다")
    void getMeeting_notFound() {

        // given
        when(
            meetingRepository
                .findByIdAndDeletedAtIsNull(10L)
        ).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingService.getMeeting(
                10L,
                1L
            )
        );
    }


    @Test
    @DisplayName("참여 중인 모임 목록을 조회할 수 있다")
    void getMyMeetings() {

        // given
        User user = createUser(
            1L,
            "user@test.com",
            "회원"
        );

        Meeting meeting1 = createMeeting(
            10L,
            user,
            "여행 모임"
        );

        Meeting meeting2 = createMeeting(
            20L,
            user,
            "스터디 모임"
        );

        MeetingMember member1 =
            new MeetingMember(
                meeting1,
                user
            );

        MeetingMember member2 =
            new MeetingMember(
                meeting2,
                user
            );

        when(userRepository.existsById(1L))
            .thenReturn(true);

        when(
            meetingMemberRepository
                .findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                    1L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(
            List.of(member1, member2)
        );

        // N+1 개선된 현재 코드에서 사용
        when(
            meetingMemberRepository
                .countByMeetingIdsAndStatus(
                    List.of(10L, 20L),
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(
            List.of(
                new Object[]{10L, 2L},
                new Object[]{20L, 4L}
            )
        );

        // 기존 방식이어도 테스트 가능하도록 설정
        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(2L);

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    20L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(4L);

        // when
        var responses =
            meetingService.getMyMeetings(1L);

        // then
        assertEquals(2, responses.size());

        assertEquals(
            "여행 모임",
            responses.get(0).name()
        );

        assertEquals(
            "스터디 모임",
            responses.get(1).name()
        );

        assertEquals(
            2L,
            responses.get(0).participantCount()
        );

        assertEquals(
            4L,
            responses.get(1).participantCount()
        );
    }

    @Test
    @DisplayName("참여 중인 모임이 없으면 빈 목록을 반환한다")
    void getMyMeetings_empty() {

        // given
        when(userRepository.existsById(1L))
            .thenReturn(true);

        when(
            meetingMemberRepository
                .findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                    1L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(List.of());

        // when
        var responses =
            meetingService.getMyMeetings(1L);

        // then
        assertEquals(0, responses.size());
    }

    @Test
    @DisplayName("존재하지 않는 회원은 참여 중인 모임 목록을 조회할 수 없다")
    void getMyMeetings_userNotFound() {

        // given
        when(userRepository.existsById(1L))
            .thenReturn(false);

        // when & then
        assertThrows(
            UserNotFoundException.class,
            () -> meetingService.getMyMeetings(1L)
        );
    }


    @Test
    @DisplayName("모임장은 진행 중인 모임 정보를 수정할 수 있다")
    void updateMeeting() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "기존 모임"
        );

        MeetingUpdateRequest request =
            new MeetingUpdateRequest(
                "수정된 모임",
                "수정된 설명"
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(3L);

        // when
        var response =
            meetingService.updateMeeting(
                10L,
                1L,
                request
            );

        // then
        assertEquals(
            "수정된 모임",
            response.name()
        );

        assertEquals(
            "수정된 설명",
            response.description()
        );
    }

    @Test
    @DisplayName("존재하지 않는 모임은 수정할 수 없다")
    void updateMeeting_notFound() {

        // given
        MeetingUpdateRequest request =
            new MeetingUpdateRequest(
                "수정된 모임",
                "수정된 설명"
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingService.updateMeeting(
                10L,
                1L,
                request
            )
        );
    }

    @Test
    @DisplayName("모임장이 아니면 모임 정보를 수정할 수 없다")
    void updateMeeting_notHost() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "기존 모임"
        );

        MeetingUpdateRequest request =
            new MeetingUpdateRequest(
                "수정된 모임",
                "수정된 설명"
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingService.updateMeeting(
                10L,
                2L,
                request
            )
        );
    }

    @Test
    @DisplayName("종료된 모임은 수정할 수 없다")
    void updateMeeting_completed() {

        // given
        User host = createUser(
            1L,
            "host@test.com",
            "모임장"
        );

        Meeting meeting = createMeeting(
            10L,
            host,
            "기존 모임"
        );

        meeting.complete();

        MeetingUpdateRequest request =
            new MeetingUpdateRequest(
                "수정된 모임",
                "수정된 설명"
            );

        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingNotActiveException.class,
            () -> meetingService.updateMeeting(
                10L,
                1L,
                request
            )
        );
    }


    @Test
    @DisplayName("지출 내역이 없으면 정산 없이 모임을 종료할 수 있다")
    void completeMeeting_withoutExpense() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            expenseRepository
                .existsByMeetingId(10L)
        ).thenReturn(false);

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(3L);

        // when
        var response =
            meetingService.completeMeeting(
                10L,
                1L
            );

        // then
        assertEquals(
            MeetingStatus.COMPLETED,
            response.status()
        );
    }

    @Test
    @DisplayName("지출 내역이 있고 정산이 완료되었다면 모임을 종료할 수 있다")
    void completeMeeting_withClosedSettlement() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            expenseRepository
                .existsByMeetingId(10L)
        ).thenReturn(true);

        when(
            settlementRepository
                .existsByMeetingIdAndStatus(
                    10L,
                    SettlementStatus.CLOSED
                )
        ).thenReturn(true);

        when(
            meetingMemberRepository
                .countByMeetingIdAndStatus(
                    10L,
                    MeetingMemberStatus.JOINED
                )
        ).thenReturn(3L);

        // when
        var response =
            meetingService.completeMeeting(
                10L,
                1L
            );

        // then
        assertEquals(
            MeetingStatus.COMPLETED,
            response.status()
        );
    }

    @Test
    @DisplayName("지출 내역이 있지만 정산이 완료되지 않았다면 모임을 종료할 수 없다")
    void completeMeeting_settlementNotCompleted() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            expenseRepository
                .existsByMeetingId(10L)
        ).thenReturn(true);

        when(
            settlementRepository
                .existsByMeetingIdAndStatus(
                    10L,
                    SettlementStatus.CLOSED
                )
        ).thenReturn(false);

        // when & then
        assertThrows(
            MeetingSettlementNotCompletedException.class,
            () -> meetingService.completeMeeting(
                10L,
                1L
            )
        );

        assertEquals(
            MeetingStatus.ACTIVE,
            meeting.getStatus()
        );
    }

    @Test
    @DisplayName("모임장이 아니면 모임을 종료할 수 없다")
    void completeMeeting_notHost() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingAccessDeniedException.class,
            () -> meetingService.completeMeeting(
                10L,
                2L
            )
        );
    }

    @Test
    @DisplayName("이미 종료된 모임은 다시 종료할 수 없다")
    void completeMeeting_alreadyCompleted() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        // when & then
        assertThrows(
            MeetingNotActiveException.class,
            () -> meetingService.completeMeeting(
                10L,
                1L
            )
        );
    }

    @Test
    @DisplayName("존재하지 않는 모임은 종료할 수 없다")
    void completeMeeting_notFound() {

        // given
        when(
            meetingRepository
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.empty());

        // when & then
        assertThrows(
            MeetingNotFoundException.class,
            () -> meetingService.completeMeeting(
                10L,
                1L
            )
        );
    }

    @Test
    @DisplayName("진행 중인 일정 투표가 있으면 모임을 종료할 수 없다")
    void completeMeeting_openSchedulePoll() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            schedulePollRepository
                .existsByMeetingIdAndStatus(
                    10L,
                    SchedulePollStatus.OPEN
                )
        ).thenReturn(true);

        assertThrows(
            MeetingPollNotCompletedException.class,
            () -> meetingService.completeMeeting(
                10L,
                1L
            )
        );

        assertEquals(
            MeetingStatus.ACTIVE,
            meeting.getStatus()
        );
    }

    @Test
    @DisplayName("진행 중인 콘텐츠 투표가 있으면 모임을 종료할 수 없다")
    void completeMeeting_openContentPoll() {

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
                .findByIdAndDeletedAtIsNullForUpdate(10L)
        ).thenReturn(Optional.of(meeting));

        when(
            contentPollRepository
                .existsByMeetingIdAndStatus(
                    10L,
                    ContentPollStatus.OPEN
                )
        ).thenReturn(true);

        assertThrows(
            MeetingPollNotCompletedException.class,
            () -> meetingService.completeMeeting(
                10L,
                1L
            )
        );

        assertEquals(
            MeetingStatus.ACTIVE,
            meeting.getStatus()
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