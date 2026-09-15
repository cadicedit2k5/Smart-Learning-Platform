package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentCreateRequest(

        @NotBlank
        @Size(max = 255)
        String title,

        @Size(max = 10000)
        String description,

        @NotNull
        Instant dueAt,

        @NotNull
        @DecimalMin("0.1")
        BigDecimal maxScore
) {
}
