package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AnnouncementUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*")
        @Size(max = 255)
        String title,

        @Pattern(regexp = "(?s).*\\S.*")
        @Size(max = 10000)
        String content
) {
}
