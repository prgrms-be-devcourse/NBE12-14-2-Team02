package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.request.MeetingUpdateRequest;
import com.prgms.backend.domain.meeting.dto.response.MeetingResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotActiveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.UserNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingSettlementNotCompletedException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final UserRepository userRepository;
    private final SettlementRepository settlementRepository;
    private final ExpenseRepository expenseRepository;

    // 모임 객체 생성
    @Transactional
    public MeetingResponse createMeeting(
        Long userId,
        MeetingCreateRequest request
    ){
        // 모임장이 존재하는 회원인지 검사
        User host = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        // 모임 객체 생성
        Meeting meeting = new Meeting(
            host,
            request.name(),
            request.description()
        );

        // DB에 모임 객체 저장
        Meeting savedMeeting = meetingRepository.save(meeting);

        // 모임장으로 모임원 객체 생성
        MeetingMember hostMember = new MeetingMember(
            savedMeeting,
            host
        );

        // DB에 모임장 객체 저장
        meetingMemberRepository.save(hostMember);

        // MeetingResponse 형태로 변환해서 리턴
        return toResponse(savedMeeting);
    }

    // 모임 상세 조회
    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId, Long userId) {

        // ACTIVE/COMPLETED 상태의 모임 상세 조회 가능, soft deleted 상태의 모임 조회 불가능
        Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // status 값까지 넣어서 현재 ACTIVE 상태로 모임에 참여 중인 모임원인지 검사
        boolean isMember =
            meetingMemberRepository.existsByMeetingIdAndUserIdAndStatus(
                meetingId,
                userId,
                MeetingMemberStatus.JOINED
            );

        // 현재 참여 중인 모임원이 아닌 경우 예외 처리
        if (!isMember) {
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        return toResponse(meeting);
    }

    // 참여 중인 모임 목록 조회
    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings(Long userId){

        // 존재하지 않는 회원인 경우
        if(!userRepository.existsById(userId)){
            throw new UserNotFoundException(userId);
        }

        return meetingMemberRepository
            .findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(
                userId,
                MeetingMemberStatus.JOINED
            )
            .stream()
            .map(MeetingMember::getMeeting)
            .map(this::toResponse)
            .toList();
    }

    // 모임 수정
    @Transactional
    public MeetingResponse updateMeeting(
        Long meetingId,
        Long userId,
        MeetingUpdateRequest request
    ){
        // 진행 중인 미팅인지 검사
        Meeting meeting = meetingRepository.findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 모임장이 아닌 참여자가 모임 수정을 시도하는 경우
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meetingId);
        }

        meeting.update(
            request.name(),
            request.description()
        );

        return toResponse(meeting);
    }

    // 모임 종료
    @Transactional
    public MeetingResponse completeMeeting(
        Long meetingId,
        Long userId
    ) {
        // 존재하며 soft delete되지 않은 모임인지 확인
        Meeting meeting = meetingRepository
            .findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 모임장만 종료 가능
        if (!meeting.isHost(userId)) {
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        // ACTIVE 상태의 모임만 종료 가능
        if (!meeting.isActive()) {
            throw new MeetingNotActiveException(meetingId);
        }

        // 지출 내역 존재 여부 확인
        boolean hasExpense =
            expenseRepository.existsByMeetingId(meetingId);

        // 지출 내역이 있는 경우에만 최종 정산 완료 여부 확인
        if (hasExpense) {

            boolean settlementCompleted =
                settlementRepository.findByMeetingId(meetingId)
                    .map(settlement ->
                        settlement.getStatus() == SettlementStatus.CLOSED
                    )
                    .orElse(false);

            if (!settlementCompleted) {
                throw new MeetingSettlementNotCompletedException(meetingId);
            }
        }

        // 지출이 없거나 최종 정산이 완료되었다면 모임 종료
        meeting.complete();

        return toResponse(meeting);
    }

    private MeetingResponse toResponse(Meeting meeting) {
        long participantCount =
            meetingMemberRepository.countByMeetingIdAndStatus(
                meeting.getId(),
                MeetingMemberStatus.JOINED
            );

        return MeetingResponse.from(meeting, participantCount);
    }
}
