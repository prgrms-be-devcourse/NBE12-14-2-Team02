package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.request.MeetingCreateRequest;
import com.prgms.backend.domain.meeting.dto.response.MeetingResponse;
import com.prgms.backend.domain.meeting.entity.Meeting;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.domain.user.entity.User;
import com.prgms.backend.domain.user.repository.UserRepository;
import com.prgms.backend.global.exception.custom.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingMemberRepository meetingMemberRepository;
    private final UserRepository userRepository;

    // 모임 객체 생성
    @Transactional
    public MeetingResponse createMeeting(
        Long hostId,
        MeetingCreateRequest request
    ){
        // 모임장이 존재하는 회원인지 검사
        User host = userRepository.findById(hostId)
            .orElseThrow(() -> new UserNotFoundException(hostId));

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
}
