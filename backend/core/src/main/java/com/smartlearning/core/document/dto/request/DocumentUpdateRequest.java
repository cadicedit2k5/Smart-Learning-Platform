package com.smartlearning.core.document.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentUpdateRequest(
        @NotBlank(
                message = "Tiêu đề không được để trống"
        )
        @Size(max = 255, message = "Tiêu đề phải ngắn hơn 255 ký tự")
        String title,

        @Size(max = 10_000, message = "Mô tả không được vượt quá 10000 ký tự")
        String description
) {
}
