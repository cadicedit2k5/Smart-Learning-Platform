package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.AssignmentSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, UUID> {

    Optional<AssignmentSubmission>
    findByAssignmentIdAndStudentId(
            UUID assignmentId,
            UUID studentId
    );

    List<AssignmentSubmission>
    findAllByStudentIdAndAssignmentCourseIdAndAssignmentDeletedAtIsNull(
            UUID studentId,
            UUID courseId
    );

    List<AssignmentSubmission>
    findAllByAssignmentIdOrderBySubmittedAtDesc(
            UUID assignmentId
    );

    Optional<AssignmentSubmission>
    findByIdAndAssignmentId(
            UUID submissionId,
            UUID assignmentId
    );

    boolean existsByAssignmentId(UUID assignmentId);
}
