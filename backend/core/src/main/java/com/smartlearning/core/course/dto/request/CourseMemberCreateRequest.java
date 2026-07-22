package com.smartlearning.core.course.dto.request;

import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CourseMemberCreateRequest(

        @NotNull(message = "ID người dùng không được để trống")
        UUID userId,

        @NotNull(message = "Vai trò không được để trống")
        CourseMemberRole role
) {
}