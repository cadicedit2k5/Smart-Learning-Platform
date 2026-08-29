package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CourseTopicCreateRequest(
        @NotBlank(message = "Tên chủ đề không được để trống")
        @Size(max = 255, message = "Tên chủ đề không được vượt quá 255 ký tự")
        String title,

        @Size(max = 10_000, message = "Mô tả không được vượt quá 10000 ký tự")
        String description,

        @PositiveOrZero(message = "Thứ tự chủ đề không được là số âm")
        Integer orderIndex,

        @Positive(message = "Thời lượng dự kiến phải lớn hơn 0")
        Integer estimatedMinutes
) {
}
