package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationDetailResponse;
import com.prgms.backend.domain.meeting.dto.response.MeetingInvitationResponse;
import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingInvitation;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingInvitationRepository;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.UserNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.AlreadyMeetingMemberException;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingInvitationExpiredException;
import com.prgms.backend.global.exception.custom.meeting.MeetingInvitationNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
    private final MeetingMemberRepository meetingMemberRepository;
    private final UserRepository userRepository;

    // 초대 기간 만료일 상수
    private static final long INVITATION_EXPIRE_DAYS = 7;

    // 초대 저장
    @Transactional
    public MeetingInvitationResponse createInvitation(
        Long meetingId,
        Long userId
    ){
        // 없는 미팅에 대한 초대인지 검사
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(
                () -> new MeetingNotFoundException(meetingId)
            );

        // 모임장이 아닌 사람이 초대를 생성하는지 검사
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
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

    // 초대를 통한 모임 참여
    @Transactional
    public MeetingMemberResponse joinMeeting(
        String inviteCode,
        Long userId
    ) {
        // 존재하는 초대인지 검사
        MeetingInvitation invitation =
            meetingInvitationRepository.findByInviteCode(inviteCode)
                .orElseThrow(
                    () -> new MeetingInvitationNotFoundException(inviteCode)
                );

        // 만료된 초대를 통해 참여하는 경우
        if (invitation.isExpired()) {
            throw new MeetingInvitationExpiredException();
        }

        // 존재하는 회원인지 검사
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // 초대에서 미팅 객체 가져오기
        Meeting meeting = invitation.getMeeting();

        // 해당 모임의 모임원 목록에 있는 회원인지 검사
        Optional<MeetingMember> existingMember =
            meetingMemberRepository.findByMeetingIdAndUserId(
                meeting.getId(),
                userId
            );

        MeetingMember meetingMember;

        // 모임원 목록에 있는 회원인 경우
        if (existingMember.isPresent()) {
            meetingMember = existingMember.get();

            // 현재 참여 중인 모임원인 경우
            if (meetingMember.isJoined()) {
                throw new AlreadyMeetingMemberException(
                    meeting.getId(),
                    userId
                );
            }

            // 현재 참여 중이 아니면(LEFT) 재가입
            meetingMember.rejoin();

            // 모임에 가입한 적 없는 회원인 경우 모임원에 등록
        } else {
            meetingMember = new MeetingMember(meeting, user);
            meetingMemberRepository.save(meetingMember);
        }

        return MeetingMemberResponse.from(meetingMember);
    }

    // 초대 코드 목록 조회(모임장만 가능)
    public List<MeetingInvitationResponse> getInvitations(
        Long meetingId,
        Long userId
    ) {
        // 존재하는 모임인지 검사
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 초대 코드 목록 조회를 시도하는 사람이 모임장인지 검사
        if (!meeting.isHost(userId)) {
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        return meetingInvitationRepository
            .findAllByMeetingId(meetingId)
            .stream()
            .map(MeetingInvitationResponse::from)
            .toList();
    }

    // 초대받은 모임 정보 조회
    public MeetingInvitationDetailResponse getInvitation(
        String inviteCode,
        Long userId
    ) {
        // 존재하는 초대인지 검사
        MeetingInvitation invitation = meetingInvitationRepository
            .findByInviteCode(inviteCode)
            .orElseThrow(
                () -> new MeetingInvitationNotFoundException(inviteCode)
            );

        // 만료된 초대인지 검사
        if (invitation.isExpired()) {
            throw new MeetingInvitationExpiredException();
        }

        Long meetingId = invitation.getMeeting().getId();

        // 현재 참여 중인 모임원인지 검사
        boolean alreadyJoined =
            meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId,
                userId,
                MeetingMemberStatus.JOINED
            );

        return MeetingInvitationDetailResponse.from(
            invitation,
            alreadyJoined
        );
    }

    // 초대 코드 삭제
    @Transactional
    public void deleteInvitation(
        Long meetingId,
        Long invitationId,
        Long userId
    ) {
        // 존재하는 모임인지 검사
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 모임장만 초대 취소 가능
        if (!meeting.isHost(userId)) {
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        // 존재하는 초대인지 검사
        MeetingInvitation invitation = meetingInvitationRepository
            .findById(invitationId)
            .orElseThrow(() -> new MeetingInvitationNotFoundException(invitationId));

        // 다른 모임의 초대 코드를 삭제하려는 경우
        if (!invitation.getMeeting().getId().equals(meetingId)) {
            throw new MeetingInvitationNotFoundException(invitationId);
        }

        meetingInvitationRepository.delete(invitation);
    }
}
