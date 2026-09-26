package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.NotNull;

public record CourseFeatureConfigRequest(
        @NotNull Boolean announcements,
        @NotNull Boolean content,
        @NotNull Boolean assignments,
        @NotNull Boolean documents,
        @NotNull Boolean discussion,
        @NotNull Boolean aiTutor
) {
}