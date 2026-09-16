package com.prgms.backend.domain.schedule.candidate.service;

import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateRequest;
import com.prgms.backend.domain.schedule.candidate.dto.ScheduleCandidateResponse;
import com.prgms.backend.domain.schedule.candidate.entity.ScheduleCandidate;
import com.prgms.backend.domain.schedule.candidate.repository.ScheduleCandidateRepository;
import com.prgms.backend.domain.schedule.poll.entity.SchedulePoll;
import com.prgms.backend.domain.schedule.poll.repository.SchedulePollRepository;
import com.prgms.backend.domain.schedule.vote.repository.ScheduleVoteRepository;
import com.prgms.backend.global.exception.custom.schedule.DuplicateScheduleCandidateException;
import com.prgms.backend.global.exception.custom.schedule.ScheduleCandidateHasVotesException;
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
    private final ScheduleVoteRepository scheduleVoteRepository;


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
        if(schedulePoll.hasDuplicateDate(
                candidateDate
        )){
            throw new DuplicateScheduleCandidateException(
                    candidateDate
            );
        }

        //후보를 SchedulePoll에 저장
        ScheduleCandidate candidate = schedulePoll.addCandidate(candidateDate);

        //save호출 시점에 insert발생
        ScheduleCandidate savedCandidate =
                scheduleCandidateRepository.save(candidate);

        return ScheduleCandidateResponse.Summary.from(savedCandidate);



    }

    @Transactional
    public ScheduleCandidateResponse.Summary update(Long meetingId, Long candidateId, ScheduleCandidateRequest.Update request) {
        //모임에 투표가 있는 지 검증
        SchedulePoll schedulePoll =
                schedulePollRepository.findByMeetingId(meetingId)
                        .orElseThrow(
                                () -> new SchedulePollNotFoundException(meetingId)
                        );
        schedulePoll.validateOpen(LocalDateTime.now());

        //schedulePoll 내부에서 candidates로 수정하려는 후보가 있는 지 검증 없다면 예외
        ScheduleCandidate candidate = schedulePoll.findCandidate(candidateId);

        //투표가 있는 지 검증 , candidateId로만 검증하는 이유는
        //이미 candidate는 schedulePoll에 소속되어있다는게 검증이 된 상태임.
        //근데 schedulePoll은 meeting이 있어야 가능하니까 meeting도 검증이 된 상태.
        //그니까 meeting의 schedulePoll의 candidate의 vote가 겹치지않는다는게 확정
        if (scheduleVoteRepository
                .existsByScheduleCandidateId(candidateId)) {
            throw new ScheduleCandidateHasVotesException(
                    candidateId
            );
        }

        LocalDate candidateDate = request.candidateDate();

        if(schedulePoll.hasDuplicateDate(
                candidateId,
                candidateDate
        )){
            throw new DuplicateScheduleCandidateException(
                    candidateDate
            );
        }

        //이미 영속성 컨텍스트 안에 있으니까 반영..
        candidate.updateCandidateDate(candidateDate);

        return ScheduleCandidateResponse.Summary.from(candidate);

    }

}
