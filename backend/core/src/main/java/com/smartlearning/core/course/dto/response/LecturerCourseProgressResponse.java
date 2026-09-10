package com.smartlearning.core.course.dto.response;

import java.util.List;
import java.util.UUID;

public record LecturerCourseProgressResponse(
        UUID courseId,
        long totalStudents,
        long totalTopics,
        int averageProgressPercentage,
        long completedStudents,
        List<StudentSummary> students
) {
    public record StudentSummary(
            UUID studentId,
            String fullName,
            String email,
            long totalTopics,
            long completedTopics,
            long inProgressTopics,
            int progressPercentage
    ) {
    }
}
