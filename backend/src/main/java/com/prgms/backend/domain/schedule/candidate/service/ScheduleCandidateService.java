package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.global.exception.custom.schedule.DuplicateScheduleCandidateException;
import com.prgms.backend.global.exception.custom.schedule.SchedulePollNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ScheduleCandidateService {
    private final ScheduleCandidateRepository scheduleCandidateRepository;
    private final SchedulePollRepository schedulePollRepository;


    //일정 후보 등록하고 SchedulePoll에다가 add하면, db반영.
    @Transactional
    public ScheduleCandidateResponse.Summary create(Long meetingId, ScheduleCandidateRequest.Create request) {

        //일정 투표가 존재한다면 모임은 반드시 존재하기때문에 일정투표만 조회.
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingId(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );

        schedulePoll.validateOpen(LocalDateTime.now());

        LocalDate candidateDate = request.candidateDate();

        //후보 날짜 검증
        //같은 날짜 있는지?(중복)
        //현재보다 이후인지는 valid에서 검증
        if(scheduleCandidateRepository.existsBySchedulePollIdAndCandidateDate(schedulePoll.getId(),candidateDate)){
            throw new DuplicateScheduleCandidateException(candidateDate);
        }




        //후보를 SchedulePoll에 저장
        ScheduleCandidate candidate = schedulePoll.addCandidate(request.candidateDate());

        //save호출 시점에 insert발생
        ScheduleCandidate savedCandidate =
                scheduleCandidateRepository.save(candidate);

        return ScheduleCandidateResponse.Summary.from(savedCandidate);



    }
}
