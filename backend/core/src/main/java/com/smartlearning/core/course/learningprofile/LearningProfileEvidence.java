package com.smartlearning.core.course.learningprofile;

import com.smartlearning.core.course.entity.*;

import java.util.List;
import java.util.UUID;

public record LearningProfileEvidence(
        UUID courseId,
        UUID studentId,
        CourseMember membership,
        long totalTopics,
        List<LearningProgress> progress,
        List<AssessmentAttempt> quizAttempts,
        long totalAssignments,
        List<AssignmentSubmission> assignmentSubmissions
) {
}