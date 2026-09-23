package com.prgms.backend.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetingCreateRequest(

    @NotBlank(message = "모임 이름은 필수입니다.")
    @Size(max = 100)
    String name,

    @Size(max = 500)
    String description
) {

}
