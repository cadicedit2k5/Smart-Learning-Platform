package com.smartlearning.core.course.dto.response;

import com.smartlearning.common.dto.response.pagination.PagingResponse;

import java.util.UUID;

public record LecturerCourseProgressResponse(
        UUID courseId,
        long totalStudents,
        long totalTopics,
        int averageProgressPercentage,
        long completedStudents,
        PagingResponse<StudentSummary> students
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