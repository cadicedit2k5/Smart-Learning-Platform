package com.smartlearning.core.course.dto.request;

import com.smartlearning.core.course.entity.enums.CourseVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CourseCreateRequest(

        @NotBlank(message = "Tên môn học không được để trống")
        @Size(
                max = 255,
                message = "Tên môn học không được vượt quá 255 ký tự"
        )
        String title,

        @Size(
                max = 5000,
                message = "Mô tả không được vượt quá 5000 ký tự"
        )
        String description,

        @Size(
                max = 50,
                message = "Cấp độ không được vượt quá 50 ký tự"
        )
        String level,

        CourseVisibility visibility
) {
}
