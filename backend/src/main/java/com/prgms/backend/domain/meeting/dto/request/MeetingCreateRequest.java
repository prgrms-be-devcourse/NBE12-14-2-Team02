package com.prgms.backend.domain.meeting.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MeetingCreateRequest(

    @NotBlank(message = "모임 이름은 필수입니다.")
    @Size(max = 100, message = "모임 이름은 100자 이하로 입력해주세요.")
    String name,

    @Size(max = 500, message = "모임 설명은 500자 이하로 입력해주세요.")
    String description
) {

}
