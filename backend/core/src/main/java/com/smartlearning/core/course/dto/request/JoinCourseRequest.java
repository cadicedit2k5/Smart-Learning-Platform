package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record JoinCourseRequest(

        @NotNull(message = "ID môn học không được để trống")
        UUID courseId,

        @NotBlank(message = "Mã tham gia không được để trống")
        @Size(max = 50)
        String code
) {
}