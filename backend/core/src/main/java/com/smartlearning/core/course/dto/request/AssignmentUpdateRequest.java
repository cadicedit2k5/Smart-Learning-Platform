package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public record AssignmentUpdateRequest(

        @Pattern(regexp = "(?s).*\\S.*")
        @Size(max = 255)
        String title,

        @Size(max = 10000)
        String description,

        Instant dueAt,

        @DecimalMin("0.1")
        BigDecimal maxScore
) {
}