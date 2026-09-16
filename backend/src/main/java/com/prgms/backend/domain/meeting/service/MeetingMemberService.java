package com.prgms.backend.domain.meeting.service;

import com.prgms.backend.domain.meeting.dto.response.MeetingMemberResponse;
import com.prgms.backend.domain.meeting.entity.MeetingMember;
import com.prgms.backend.domain.meeting.enums.MeetingMemberStatus;
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
        // 존재하는 모임원인지 검사
        MeetingMember meetingMember = meetingMemberRepository
            .findById(meetingMemberId)
            .orElseThrow(
                () -> new MeetingMemberNotFoundException(meetingMemberId)
            );

        // MeetingMemberResponse 형태로 변환해서 리턴
        return MeetingMemberResponse.from(meetingMember);
    }

    // 특정 모임에서 특정 사용자 조회
    public List<MeetingMemberResponse> getMeetingMembers(Long meetingId){
        // 모임 id로 가져온 모임원 객체들을 MeetingMemberResponse 형태로 변환해서 리스트 리턴
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
}
