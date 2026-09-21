package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.request.MeetingUpdateRequest;
import com.prgms.backend.domain.meeting.dto.response.MeetingResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.meeting.MeetingAccessDeniedException;
import com.prgms.backend.global.exception.custom.meeting.MeetingNotFoundException;
import com.prgms.backend.global.exception.custom.UserNotFoundException;
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
        return MeetingResponse.from(savedMeeting);
    }

    // 모임 상세 조회
    @Transactional(readOnly = true)
    public MeetingResponse getMeeting(Long meetingId, Long userId) {
        Meeting meeting = meetingRepository.findById(meetingId)
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

        return MeetingResponse.from(meeting);
    }

    // 참여 중인 모임 목록 조회
    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings(Long userId){

        // 존재하지 않는 회원인 경우
        if(!userRepository.existsById(userId)){
            throw new UserNotFoundException(userId);
        }

        return meetingMemberRepository
            .findAllByUserIdAndStatus(
                userId,
                MeetingMemberStatus.JOINED
            )
            .stream()
            .map(MeetingMember::getMeeting)
            .map(MeetingResponse::from)
            .toList();
    }

    // 모임 수정
    @Transactional
    public MeetingResponse updateMeeting(
        Long meetingId,
        Long userId,
        MeetingUpdateRequest request
    ){
        // 존재하는 미팅인지 검사
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new MeetingNotFoundException(meetingId));

        // 모임장이 아닌 참여자가 모임 수정을 시도하는 경우
        if(!meeting.isHost(userId)){
            throw new MeetingAccessDeniedException(meetingId, userId);
        }

        meeting.update(
            request.name(),
            request.description()
        );

        return MeetingResponse.from(meeting);
    }
}
