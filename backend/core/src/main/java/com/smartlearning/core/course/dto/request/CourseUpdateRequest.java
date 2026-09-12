package com.smartlearning.core.course.dto.request;

import com.smartlearning.core.course.entity.enums.CourseVisibility;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record CourseUpdateRequest(
        @Pattern(regexp = "(?s).*\\S.*", message = "Tên môn học không được để trống")
        @Size(max = 255, message = "Tên môn học không được vượt quá 255 ký tự")
        String title,

        @Size(max = 50_000, message = "Nội dung giới thiệu khóa học quá dài")
        String description,

        @Size(max = 50, message = "Cấp độ không được vượt quá 50 ký tự")
        String level,

        CourseVisibility visibility,
        MultipartFile image
) {
        public CourseUpdateRequest(String title, String description, String level, CourseVisibility visibility) {
                this(title, description, level, visibility, null);
        }

        public CourseUpdateRequest withImage(MultipartFile image) {
                return new CourseUpdateRequest(title, description, level, visibility, image);
        }
}