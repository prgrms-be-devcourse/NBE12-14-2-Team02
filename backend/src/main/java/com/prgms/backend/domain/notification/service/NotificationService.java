package com.prgms.backend.domain.notification.service;

import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.notification.ENUM.NotificationType;
import com.prgms.backend.domain.notification.dto.response.NotificationResponse;
import com.prgms.backend.domain.notification.entity.Notification;
import com.prgms.backend.domain.notification.repository.NotificationRepository;
import com.prgms.backend.global.exception.custom.notification.NotificationAccessDeniedException;
import com.prgms.backend.global.exception.custom.notification.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final MeetingMemberRepository meetingMemberRepository;

    @Transactional
    public void notifyContentClosed(Long meetingId){
        notifyAllJoin(
                meetingId,
                NotificationType.CONTENT_CLOSED,
                "콘텐츠 투표가 마감되었습니다.",
                "결과를 확인해 주세요.",
                "/meetings/"+ meetingId +"/content-poll"
        );
    }

    @Transactional
    public void notifyScheduleClosed(Long meetingId){
        notifyAllJoin(
                meetingId,
                NotificationType.SCHEDULE_CLOSED,
                "일정 투표가 마감되었습니다.",
                "결과를 확인해 주세요.",
                "/meetings/"+ meetingId +"/schedule-poll"
        );
    }

    @Transactional
    public void notifySettlementClosed(Long meetingId){
        notifyAllJoin(
                meetingId,
                NotificationType.SETTLEMENT_CLOSED,
                "정산이 마감되었습니다.",
                "정산 내역을 확인해 주세요.",
                "/meetings/"+ meetingId +"/settlement-poll"
        );
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getMyNotifications(Long userId){
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponse::from)
                .toList();
    }

    @Transactional
    public NotificationResponse markRead(Long notificationId, Long userId){
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));
        if(!notification.getReceiverId().equals(userId)){
            throw new NotificationAccessDeniedException(notificationId,userId);
        }
        notification.markRead();
        return NotificationResponse.from(notification);
    }

    //미팅 멤버들에게 모두 알림 보냄
    private void notifyAllJoin(
            Long meetingId,
            NotificationType type,
            String title,
            String content,
            String redirectUrl
    ){
        List<MeetingMember> members = meetingMemberRepository
                .findAllByMeetingIdAndStatus(meetingId, MeetingMemberStatus.JOINED);
        for(MeetingMember member : members){
            notificationRepository.save(new Notification(
                    member.getUser().getId(),
                    meetingId,
                    type,
                    title,
                    content,
                    redirectUrl,
                    false
            ));
        }
    }
}
