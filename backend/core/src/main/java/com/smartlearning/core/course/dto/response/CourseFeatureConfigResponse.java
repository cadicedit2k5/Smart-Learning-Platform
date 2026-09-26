package com.smartlearning.core.course.dto.response;

public record CourseFeatureConfigResponse(
        boolean announcements,
        boolean content,
        boolean assignments,
        boolean documents,
        boolean discussion,
        boolean aiTutor
) {
    public static CourseFeatureConfigResponse allEnabled() {
        return new CourseFeatureConfigResponse(true, true, true, true, true, true);
    }
}