package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CourseMemberCreateRequest(

        @NotNull(message = "ID người dùng không được để trống")
        UUID userId
) {
}
