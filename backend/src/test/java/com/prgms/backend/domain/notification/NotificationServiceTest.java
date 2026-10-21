package com.prgms.backend.domain.notification;

import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.notification.ENUM.NotificationType;
import com.prgms.backend.domain.notification.dto.response.NotificationResponse;
import com.prgms.backend.domain.notification.entity.Notification;
import com.prgms.backend.domain.notification.repository.NotificationRepository;
import com.prgms.backend.domain.notification.service.NotificationService;
import com.prgms.backend.global.exception.custom.notification.NotificationAccessDeniedException;
import com.prgms.backend.global.exception.custom.notification.NotificationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.meeting;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.member;
import static com.prgms.backend.domain.schedule.support.ScheduleTestFixtures.user;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class NotificationServiceTest {

    private static final long MEETING_ID = 10L;

    private NotificationRepository notificationRepository;
    private MeetingMemberRepository meetingMemberRepository;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        notificationRepository = mock(NotificationRepository.class);
        meetingMemberRepository = mock(MeetingMemberRepository.class);
        service = new NotificationService(notificationRepository, meetingMemberRepository);

        Meeting meeting = meeting(MEETING_ID, user(1L, "host"));
        MeetingMember host = member(30L, meeting, meeting.getHost());
        MeetingMember guest = member(31L, meeting, user(2L, "guest"));
        when(meetingMemberRepository.findAllByMeetingIdAndStatus(MEETING_ID, MeetingMemberStatus.JOINED))
                .thenReturn(List.of(host, guest));
    }

    private List<Notification> savedNotifications(int expectedCount) {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, times(expectedCount)).save(captor.capture());
        return captor.getAllValues();
    }

    private Notification notification(long id, long receiverId) {
        Notification notification = new Notification(receiverId, MEETING_ID, NotificationType.CONTENT_CLOSED,
                "제목", "내용", "/url", false);
        ReflectionTestUtils.setField(notification, "id", id);
        return notification;
    }

    @Test
    @DisplayName("콘텐츠 투표 마감 알림을 참여 중인 모든 멤버에게 저장")
    void notifyContentClosed() {

        // when
        service.notifyContentClosed(MEETING_ID);

        // then
        List<Notification> saved = savedNotifications(2);
        assertEquals(List.of(1L, 2L), saved.stream().map(Notification::getReceiverId).toList());
        Notification first = saved.get(0);
        assertEquals(NotificationType.CONTENT_CLOSED, first.getType());
        assertEquals(MEETING_ID, first.getMeetingId());
        assertEquals("콘텐츠 투표가 마감되었습니다.", first.getTitle());
        assertEquals("/meetings/10/content-poll", first.getRedirectUrl());
        assertFalse(first.isRead());
    }

    @Test
    @DisplayName("일정 투표 마감 알림은 일정 투표 타입과 경로로 저장")
    void notifyScheduleClosed() {

        // when
        service.notifyScheduleClosed(MEETING_ID);

        // then
        Notification first = savedNotifications(2).get(0);
        assertEquals(NotificationType.SCHEDULE_CLOSED, first.getType());
        assertEquals("일정 투표가 마감되었습니다.", first.getTitle());
        assertEquals("/meetings/10/schedule-poll", first.getRedirectUrl());
    }

    @Test
    @DisplayName("정산 마감 알림은 정산 타입과 경로로 저장")
    void notifySettlementClosed() {

        // when
        service.notifySettlementClosed(MEETING_ID);

        // then
        Notification first = savedNotifications(2).get(0);
        assertEquals(NotificationType.SETTLEMENT_CLOSED, first.getType());
        assertEquals("정산이 마감되었습니다.", first.getTitle());
        assertEquals("/meetings/10/settlement-poll", first.getRedirectUrl());
    }

    @Test
    @DisplayName("참여 중인 멤버가 없으면 알림을 저장하지 않음")
    void notifyWithoutMembers() {

        // given
        when(meetingMemberRepository.findAllByMeetingIdAndStatus(MEETING_ID, MeetingMemberStatus.JOINED))
                .thenReturn(List.of());

        // when
        service.notifyContentClosed(MEETING_ID);

        // then
        verify(notificationRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("내 알림 목록을 저장소가 준 순서(최신순) 그대로 반환")
    void getMyNotifications() {

        // given
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification(2L, 1L), notification(1L, 1L)));

        // when
        List<NotificationResponse> responses = service.getMyNotifications(1L);

        // then
        assertEquals(List.of(2L, 1L), responses.stream().map(NotificationResponse::id).toList());
    }

    @Test
    @DisplayName("알림이 없으면 빈 목록을 반환")
    void getMyNotificationsEmpty() {

        // given
        when(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        // when / then
        assertTrue(service.getMyNotifications(1L).isEmpty());
    }

    @Test
    @DisplayName("본인 알림은 읽음 처리")
    void markRead() {

        // given
        Notification notification = notification(5L, 1L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        // when
        NotificationResponse response = service.markRead(5L, 1L);

        // then
        assertTrue(response.isRead());
        assertTrue(notification.isRead());
    }

    @Test
    @DisplayName("존재하지 않는 알림은 읽음 처리할 수 없음")
    void markReadNotFound() {

        // given
        when(notificationRepository.findById(5L)).thenReturn(Optional.empty());

        // when / then
        assertThrows(NotificationNotFoundException.class, () -> service.markRead(5L, 1L));
    }

    @Test
    @DisplayName("다른 사람의 알림은 읽음 처리할 수 없고 읽음 상태도 바뀌지 않음")
    void markReadByOtherUser() {

        // given
        Notification notification = notification(5L, 1L);
        when(notificationRepository.findById(5L)).thenReturn(Optional.of(notification));

        // when / then
        assertThrows(NotificationAccessDeniedException.class, () -> service.markRead(5L, 2L));
        assertFalse(notification.isRead());
    }
}
