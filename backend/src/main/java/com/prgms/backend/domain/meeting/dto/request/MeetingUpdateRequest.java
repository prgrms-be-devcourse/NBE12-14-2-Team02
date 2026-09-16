package com.prgms.backend.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;

public record MeetingUpdateRequest(
    @NotBlank(message = "모임 이름은 필수입니다.")
    String name,

    String description
) {

}
