package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record AssignmentDeadlineUpdateRequest(@NotNull Instant dueAt) {
}