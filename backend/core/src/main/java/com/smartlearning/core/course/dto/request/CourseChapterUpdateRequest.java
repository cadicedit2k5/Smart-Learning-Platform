package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CourseChapterUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên chương không được để trống")
        @Size(max = 255, message = "Tên chương không được vượt quá 255 ký tự")
        String title,

        @Size(max = 10_000, message = "Mô tả không được vượt quá 10000 ký tự")
        String description,

        @Size(max = 10_000, message = "Mục tiêu học tập không được vượt quá 10000 ký tự")
        String learningObjectives,

        @PositiveOrZero(message = "Thứ tự chương không được là số âm")
        Integer orderIndex
) {
}
