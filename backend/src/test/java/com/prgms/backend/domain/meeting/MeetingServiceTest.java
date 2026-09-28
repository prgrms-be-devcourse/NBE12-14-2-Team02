package com.prgms.backend.domain.meeting;

import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.meeting.service.MeetingService;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private MeetingService meetingService;

    @BeforeEach
    void setUp() {
        meetingRepository = mock(MeetingRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        userRepository = mock(UserRepository.class);
        settlementRepository = mock(SettlementRepository.class);
        expenseRepository = mock(ExpenseRepository.class);

        meetingService = new MeetingService(
            meetingRepository,
            meetingMemberRepository,
            userRepository,
            settlementRepository,
            expenseRepository
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
}