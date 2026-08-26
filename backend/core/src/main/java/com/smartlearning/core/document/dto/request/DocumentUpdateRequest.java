package com.smartlearning.core.document.dto.request;

import com.smartlearning.core.document.entity.enums.DocumentLifecycleStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record DocumentUpdateRequest(
        @Pattern(
                regexp = "(?s).*\\S.*",
                message = "Tiêu đề không được để trống"
        )
        @Size(max = 255, message = "Tiêu đề phải ngắn hơn 255 ký tự")
        String title,

        @Size(max = 10_000, message = "Mô tả không được vượt quá 10000 ký tự")
        String description,

        DocumentLifecycleStatus lifecycleStatus,

        UUID chapterId,

        UUID topicId
) {
}
