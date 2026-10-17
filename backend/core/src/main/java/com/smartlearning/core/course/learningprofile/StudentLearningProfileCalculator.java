package com.smartlearning.core.course.learningprofile;

import com.smartlearning.core.course.dto.response.StudentLearningProfileResponse;
import com.smartlearning.core.course.entity.*;
import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

@Component
public class StudentLearningProfileCalculator {

    public StudentLearningProfileResponse calculate(
            LearningProfileEvidence evidence
    ) {
        return new StudentLearningProfileResponse(
                evidence.courseId(),
                evidence.studentId(),
                evidence.membership().getJoinedAt(),
                calculateProgress(evidence),
                calculateEngagement(evidence),
                calculateFormative(evidence.quizAttempts()),
                calculateFormal(
                        evidence.totalAssignments(),
                        evidence.assignmentSubmissions()
                )
        );
    }

    private StudentLearningProfileResponse.Progress calculateProgress(
            LearningProfileEvidence evidence
    ) {
        long completed = evidence.progress().stream()
                .filter(item -> item.getStatus() == LearningProgressStatus.COMPLETED)
                .count();

        long inProgress = evidence.progress().stream()
                .filter(item -> item.getStatus() == LearningProgressStatus.IN_PROGRESS)
                .count();

        long notStarted = evidence.totalTopics() - completed - inProgress;

        return new StudentLearningProfileResponse.Progress(
                evidence.totalTopics(),
                completed,
                inProgress,
                notStarted,
                percentage(completed, evidence.totalTopics())
        );
    }

    private StudentLearningProfileResponse.Engagement calculateEngagement(
            LearningProfileEvidence evidence
    ) {
        long totalActiveSeconds = evidence.progress().stream()
                .mapToLong(LearningProgress::getActiveSeconds)
                .sum();

        Instant lastActivity = null;

        for (LearningProgress progress : evidence.progress()) {
            lastActivity = latest(lastActivity, progress.getLastAccessedAt());
        }

        for (AssessmentAttempt attempt : evidence.quizAttempts()) {
            lastActivity = latest(lastActivity, attempt.getSubmittedAt());
        }

        for (AssignmentSubmission submission : evidence.assignmentSubmissions()) {
            lastActivity = latest(lastActivity, submission.getSubmittedAt());
        }

        return new StudentLearningProfileResponse.Engagement(
                totalActiveSeconds,
                lastActivity
        );
    }

    private StudentLearningProfileResponse.Formative calculateFormative(
            List<AssessmentAttempt> attempts
    ) {
        if (attempts.isEmpty()) {
            return new StudentLearningProfileResponse.Formative(
                    0,
                    null,
                    null,
                    null
            );
        }

        AssessmentAttempt first = attempts.stream()
                .min(Comparator.comparing(AssessmentAttempt::getSubmittedAt))
                .orElseThrow();

        AssessmentAttempt latest = attempts.stream()
                .max(Comparator.comparing(AssessmentAttempt::getSubmittedAt))
                .orElseThrow();

        double best = attempts.stream()
                .mapToDouble(this::quizScorePercentage)
                .max()
                .orElseThrow();

        return new StudentLearningProfileResponse.Formative(
                attempts.size(),
                roundPercentage(quizScorePercentage(first)),
                roundPercentage(quizScorePercentage(latest)),
                roundPercentage(best)
        );
    }

    private StudentLearningProfileResponse.Formal calculateFormal(
            long totalAssignments,
            List<AssignmentSubmission> submissions
    ) {
        OptionalDouble averageGradedScore = submissions.stream()
                .filter(submission ->
                        submission.getStatus() == AssignmentSubmissionStatus.GRADED &&
                                submission.getScore() != null
                )
                .mapToDouble(this::assignmentScorePercentage)
                .average();

        return new StudentLearningProfileResponse.Formal(
                percentage(submissions.size(), totalAssignments),
                averageGradedScore.isPresent()
                        ? roundPercentage(averageGradedScore.getAsDouble())
                        : null
        );
    }

    private double quizScorePercentage(AssessmentAttempt attempt) {
        return attempt.getScore() * 100.0 / attempt.getMaxScore();
    }

    private double assignmentScorePercentage(AssignmentSubmission submission) {
        return submission.getScore().doubleValue() * 100.0 /
                submission.getAssignment().getMaxScore().doubleValue();
    }

    private int percentage(long value, long total) {
        if (total == 0) return 0;
        return (int) Math.round(value * 100.0 / total);
    }

    private double roundPercentage(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private Instant latest(Instant current, Instant candidate) {
        if (current == null || candidate.isAfter(current)) return candidate;
        return current;
    }
}