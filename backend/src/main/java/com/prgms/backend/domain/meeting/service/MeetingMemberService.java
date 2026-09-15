package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.repository.MeetingMemberRepository;
import com.prgms.backend.global.exception.custom.MeetingMemberNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MeetingMemberService {

    private final MeetingMemberRepository meetingMemberRepository;

    // meetingMemberId로 멤버 한 명 조회
    public MeetingMemberResponse getMeetingMember(Long meetingMemberId){
        MeetingMember meetingMember = meetingMemberRepository
            .findById(meetingMemberId)
            .orElseThrow(
                () -> new MeetingMemberNotFoundException(meetingMemberId)
            );

        return MeetingMemberResponse.from(meetingMember);
    }

    // 특정 모임에서 특정 사용자 조회
    public List<MeetingMemberResponse> getMeetingMembers(Long meetingId){
        return meetingMemberRepository
            .findAllByMeetingId(meetingId)
            .stream()
            .map(MeetingMemberResponse::from)
            .toList();
    }

    // 특정 사용자가 모임 멤버인지 확인
    public boolean isMeetingMember(Long meetingId, Long userId){
        return meetingMemberRepository.existsByMeetingIdAndUserId(meetingId, userId);
    }
}
