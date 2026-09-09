package com.smartlearning.core.course.dto.response;

import java.util.UUID;

public record DiscussionAuthorResponse(
        UUID id,
        String fullName,
        String email
) {
}
