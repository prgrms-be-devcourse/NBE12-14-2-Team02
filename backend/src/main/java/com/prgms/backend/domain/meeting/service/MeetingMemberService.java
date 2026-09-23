package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.content.ENUM.ContentPollStatus;
import com.prgms.backend.domain.content.repository.ContentVoteRepository;
import com.prgms.backend.domain.expense.repository.ExpenseRepository;
import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePollStatus;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.domain.settlement.entity.SettlementStatus;
import com.prgms.backend.domain.settlement.repository.SettlementRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingHostCannotLeaveException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberAlreadyLeftException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberHasUnsettledExpenseException;
import com.prgms.backend.global.exception.custom.meeting.MeetingMemberNotFoundException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingMemberService {

    private final MeetingMemberRepository meetingMemberRepository;
    private final MeetingRepository meetingRepository;

    private final ExpenseRepository expenseRepository;
    private final SettlementRepository settlementRepository;

    private final ScheduleVoteRepository scheduleVoteRepository;
    private final ContentVoteRepository contentVoteRepository;

    // meetingMemberId로 멤버 한 명 조회
    public MeetingMemberResponse getMeetingMember(Long meetingMemberId){
        // 존재하는 모임원인지 검사
        MeetingMember meetingMember = meetingMemberRepository
            .findById(meetingMemberId)
            .orElseThrow(
                () -> new MeetingMemberNotFoundException(meetingMemberId)
            );

        // MeetingMemberResponse 형태로 변환해서 리턴
        return MeetingMemberResponse.from(meetingMember);
    }

    // 현재 모임원 목록 조회
    public List<MeetingMemberResponse> getMeetingMembers(
        Long meetingId,
        Long userId
    ){

        // 존재하지 않는 모임인지 검사
        meetingRepository
            .findByIdAndDeletedAtIsNull(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 현재 로그인한 사용자가 해당 모임에 참여 중인지 검사
        if(!isMeetingMember(meetingId, userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        // JOINED 상태의 모임원만 조회
        return meetingMemberRepository
            .findAllByMeetingIdAndStatus(
                meetingId,
                MeetingMemberStatus.JOINED
            )
            .stream()
            .map(MeetingMemberResponse::from)
            .toList();
    }

    // 특정 사용자가 모임 멤버인지 확인(status = JOINED인가?)
    public boolean isMeetingMember(Long meetingId, Long userId){
        return meetingMemberRepository
            .existsByMeetingIdAndUserIdAndStatus(
                meetingId,
                userId,
                MeetingMemberStatus.JOINED
                );
    }

    // 모임 탈퇴
    @Transactional
    public MeetingMemberResponse leaveMeeting(
        Long meetingId,
        Long userId
    ) {
        // 존재하는지, 삭제되지 않은 모임인지 검사
        Meeting meeting = meetingRepository
            .findByIdAndDeletedAtIsNullForUpdate(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 모임장은 모임 탈퇴 불가
        if (meeting.isHost(userId) && !meeting.isCompleted()) {
            throw new MeetingHostCannotLeaveException(meetingId, userId);
        }

        // 존재하는 모임원인지 검사
        MeetingMember meetingMember =
            meetingMemberRepository
                .findByMeetingIdAndUserId(meetingId, userId)
                .orElseThrow(() -> new MeetingMemberNotFoundException(meetingId, userId));

        // 이미 탈퇴한 회원
        if (!meetingMember.isJoined()) {
            throw new MeetingMemberAlreadyLeftException(meetingId, userId);
        }

        Long meetingMemberId = meetingMember.getId();

        // 최종 정산 완료 여부 확인
        boolean settlementCompleted =
            settlementRepository.findByMeetingId(meetingId)
                .map(settlement -> settlement.getStatus() == SettlementStatus.CLOSED)
                .orElse(false);

        // 최종 정산 전이라면 지출 참여 여부 검사
        if (!settlementCompleted) {

            boolean involvedInExpense =
                expenseRepository.existsMemberInExpense(meetingId, meetingMemberId);

            if (involvedInExpense) {
                throw new MeetingMemberHasUnsettledExpenseException(meetingId, userId);
            }
        }

        // 같은 시각 기준으로 OPEN 투표 판단
        LocalDateTime now = LocalDateTime.now();

        // 진행 중인 일정 투표의 본인 Vote 삭제
        scheduleVoteRepository.deleteOpenVotesByMeetingMember(
            meetingId,
            meetingMemberId,
            SchedulePollStatus.OPEN,
            now
        );

        // 진행 중인 콘텐츠 투표의 본인 Vote 삭제
        contentVoteRepository.deleteOpenVotesByMeetingMember(
            meetingId,
            meetingMemberId,
            ContentPollStatus.OPEN,
            now
        );

        // JOINED → LEFT
        meetingMember.leave();

        return MeetingMemberResponse.from(meetingMember);
    }
}
