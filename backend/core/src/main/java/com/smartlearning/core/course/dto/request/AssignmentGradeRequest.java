package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AssignmentGradeRequest(

        @NotNull
        @DecimalMin("0")
        BigDecimal score,

        @Size(max = 5000)
        String feedback
) {
}