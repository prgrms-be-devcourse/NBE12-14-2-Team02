package com.prgms.backend.domain.content.service;

import com.prgms.backend.domain.content.dto.ContentPollCreateRequest;
import com.prgms.backend.domain.content.dto.ContentPollResponse;
import com.prgms.backend.domain.content.entity.ContentPoll;
import com.prgms.backend.domain.content.repository.ContentPollRepository;
import com.prgms.backend.domain.meeting.repository.MeetingRepository;
import com.prgms.backend.global.exception.custom.content.ContentPollAlreadyExistsException;
import com.prgms.backend.global.exception.custom.MeetingNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ContentPollService {
    private final ContentPollRepository contentPollRepository;
    private final MeetingRepository meetingRepository;

    @Transactional
    public ContentPollResponse create(Long meetingId, ContentPollCreateRequest request){
        if(!meetingRepository.existsById(meetingId)){
            throw new MeetingNotFoundException(meetingId);
        }

        if (contentPollRepository.existsByMeetingId(meetingId)){
            throw new ContentPollAlreadyExistsException(meetingId);
        }

        ContentPoll saved = contentPollRepository.save(
                new ContentPoll(meetingId,request.deadline())
        );

        return ContentPollResponse.from(saved);
    }
}
