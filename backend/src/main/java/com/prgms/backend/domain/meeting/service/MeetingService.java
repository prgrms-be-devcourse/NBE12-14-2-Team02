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

    // 모임 생성
    @Transactional
    public MeetingResponse createMeeting(
        Long hostId,
        MeetingCreateRequest request
    ){
        User host = userRepository.findById(hostId)
            .orElseThrow(() -> new UserNotFoundException(hostId));

        Meeting meeting = new Meeting(
            host,
            request.name(),
            request.description()
        );

        Meeting savedMeeting = meetingRepository.save(meeting);

        MeetingMember hostMember = new MeetingMember(
            savedMeeting,
            host
        );

        meetingMemberRepository.save(hostMember);

        return MeetingResponse.from(savedMeeting);
    }
}
