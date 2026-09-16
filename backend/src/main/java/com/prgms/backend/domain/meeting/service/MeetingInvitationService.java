package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.MeetingNotFoundException;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingInvitationService {

    private final MeetingRepository meetingRepository;
    private final MeetingInvitationRepository meetingInvitationRepository;

    // 초대 기간 만료일 상수
    private static final long INVITATION_EXPIRE_DAYS = 7;

    // 초대 저장
    @Transactional
    public MeetingInvitationResponse createInvitation(
        Long meetingId,
        Long hostId
    ){
        // 없는 미팅에 대한 초대인지 검사
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(
                () -> new MeetingNotFoundException(meetingId)
            );

        // 모임장이 아닌 사람이 초대를 생성하는지 검사
        if(!meeting.isHost(hostId)){
            throw new MeetingAccessDeniedException(meetingId, hostId);
        }

        // UUID 사용 - 쉽게 고유값 생성 가능, 충돌 위험 낮음, 별도 번호 생성 로직 필요X
        String inviteCode = UUID.randomUUID().toString();

        // 초대 기간 만료일 계산
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(INVITATION_EXPIRE_DAYS);

        // 초대 객체 생성
        MeetingInvitation invitation = new MeetingInvitation(
            meeting,
            inviteCode,
            expiresAt
        );

        // DB에 초대 객체 저장
        MeetingInvitation savedInvitation = meetingInvitationRepository.save(invitation);

        // MeetingInvitationResponse 형태로 변환해서 리턴
        return MeetingInvitationResponse.from(savedInvitation);
    }
}
