package com.smartlearning.core.course.dto.response;

import java.util.UUID;

public record CourseMemberUserResponse(
        UUID id,
        String email,
        String fullName
) {
}