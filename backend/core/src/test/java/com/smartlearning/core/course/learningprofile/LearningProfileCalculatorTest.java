package com.smartlearning.core.course.learningprofile;

import com.smartlearning.core.course.dto.response.StudentLearningProfileResponse;
import com.smartlearning.core.course.entity.*;
import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentLearningProfileCalculatorTest {

    private final StudentLearningProfileCalculator calculator =
            new StudentLearningProfileCalculator();

    @Test
    void calculate_buildsProfileFromLearningEvidence() {
        UUID courseId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();

        CourseMember member = new CourseMember();
        member.setUserId(studentId);
        member.setJoinedAt(Instant.parse("2026-10-01T00:00:00Z"));

        LearningProgress completed = progress(
                LearningProgressStatus.COMPLETED,
                120,
                "2026-10-10T10:00:00Z"
        );

        LearningProgress inProgress = progress(
                LearningProgressStatus.IN_PROGRESS,
                60,
                "2026-10-11T10:00:00Z"
        );

        AssessmentAttempt firstAttempt = attempt(
                1,
                2,
                "2026-10-09T10:00:00Z"
        );

        AssessmentAttempt bestAttempt = attempt(
                1,
                1,
                "2026-10-11T12:00:00Z"
        );

        AssessmentAttempt latestAttempt = attempt(
                3,
                4,
                "2026-10-12T10:00:00Z"
        );

        AssignmentSubmission firstSubmission = submission(
                AssignmentSubmissionStatus.SUBMITTED,
                null,
                10,
                "2026-10-08T10:00:00Z"
        );

        AssignmentSubmission gradedSubmission1 = submission(
                AssignmentSubmissionStatus.GRADED,
                8,
                10,
                "2026-10-13T10:00:00Z"
        );

        AssignmentSubmission gradedSubmission2 = submission(
                AssignmentSubmissionStatus.GRADED,
                15,
                20,
                "2026-10-14T10:00:00Z"
        );

        LearningProfileEvidence evidence = new LearningProfileEvidence(
                courseId,
                studentId,
                member,
                4,
                List.of(completed, inProgress),
                List.of(firstAttempt, bestAttempt, latestAttempt),
                4,
                List.of(
                        firstSubmission,
                        gradedSubmission1,
                        gradedSubmission2
                )
        );

        StudentLearningProfileResponse result =
                calculator.calculate(evidence);

        assertThat(result.progress())
                .isEqualTo(
                        new StudentLearningProfileResponse.Progress(
                                4,
                                1,
                                1,
                                2,
                                25
                        )
                );

        assertThat(result.engagement().totalActiveSeconds())
                .isEqualTo(180);

        assertThat(result.engagement().lastLearningActivity())
                .isEqualTo(
                        Instant.parse("2026-10-14T10:00:00Z")
                );

        assertThat(result.formative().attemptCount())
                .isEqualTo(3);

        assertThat(result.formative().firstScorePercentage())
                .isEqualTo(50.0);

        assertThat(result.formative().latestScorePercentage())
                .isEqualTo(75.0);

        assertThat(result.formative().bestScorePercentage())
                .isEqualTo(100.0);

        assertThat(result.formal().submissionRatePercentage())
                .isEqualTo(75);

        assertThat(result.formal().averageGradedScorePercentage())
                .isEqualTo(77.5);
    }

    private LearningProgress progress(
            LearningProgressStatus status,
            long activeSeconds,
            String lastAccessedAt
    ) {
        LearningProgress progress = new LearningProgress();
        progress.setStatus(status);
        progress.setActiveSeconds(activeSeconds);
        progress.setLastAccessedAt(Instant.parse(lastAccessedAt));
        return progress;
    }

    private AssessmentAttempt attempt(
            int score,
            int maxScore,
            String submittedAt
    ) {
        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setScore(score);
        attempt.setMaxScore(maxScore);
        attempt.setSubmittedAt(Instant.parse(submittedAt));
        return attempt;
    }

    private AssignmentSubmission submission(
            AssignmentSubmissionStatus status,
            Integer score,
            int maxScore,
            String submittedAt
    ) {
        Assignment assignment = new Assignment();
        assignment.setMaxScore(BigDecimal.valueOf(maxScore));

        AssignmentSubmission submission = new AssignmentSubmission();
        submission.setAssignment(assignment);
        submission.setStatus(status);
        submission.setScore(
                score == null
                        ? null
                        : BigDecimal.valueOf(score)
        );
        submission.setSubmittedAt(Instant.parse(submittedAt));

        return submission;
    }
}