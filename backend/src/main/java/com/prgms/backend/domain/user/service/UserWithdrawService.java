package com.prgms.backend.domain.user.service;

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
import com.prgms.backend.global.exception.custom.user.UserNotFoundException;
import com.prgms.backend.global.exception.custom.user.UserWithdrawActivateHostMeetingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class UserWithdrawService {

    private final UserRepository userRepository;
    private final MeetingRepository meetingRepository;
    private final MeetingMemberService meetingMemberService;
    private final MeetingMemberRepository meetingMemberRepository;

    // 회원 탈퇴
    public void withdraw(Long userId) {

        User user = userRepository.findById(userId).orElseThrow(
                () -> new UserNotFoundException("회원 정보가 존재하지 않습니다.")
        );

        // host로 참여, 완료되지 않고 삭제되지 않은 모임
        if (meetingRepository.existsByHostIdAndStatusAndDeletedAtIsNull(userId, MeetingStatus.ACTIVE)) {
            throw new UserWithdrawActivateHostMeetingException();
        }

        // 멤버로 참여한 모임을 leave
        leaveMeetings(userId);

        // 호스트로 등록된 모임을 soft delete
        softDeleteHostMeeting(user);

        // user 탈퇴
        withdrawUser(user);
    }

    // 호스트로 등록된 완료된 모임을 삭제 (soft delete)
    private void softDeleteHostMeeting(User user) {
        List<Meeting> meetings = meetingRepository.findByHostIdAndDeletedAtIsNull(user.getId());
        meetings.forEach(Meeting::softDelete);
    }


    // 멤버로 참여한 모임을 leave
    private void leaveMeetings(Long userId) {

        // join이고 left_at이 null인 MeetingMember 리스트
        List<MeetingMember> meetingMembers = meetingMemberRepository.findAllByUserIdAndStatusAndMeetingDeletedAtIsNull(userId, MeetingMemberStatus.JOINED);

        meetingMembers.stream()
                .map(MeetingMember::getMeeting)
                .forEach(meeting -> {
                    meetingMemberService.leaveMeeting(meeting.getId(), userId);
                });
    }

    // user을 탈퇴 처리
    private void withdrawUser(User user) {
        user.updateStatus(UserStatus.WITHDRAWN);
        user.updateDeletedAt(LocalDateTime.now());
        user.updateRefreshToken(null);
    }
}
