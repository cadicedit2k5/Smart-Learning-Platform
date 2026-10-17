package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.AssignmentSubmission;
import com.smartlearning.core.course.entity.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
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

    @Query("""
        select submission
        from AssignmentSubmission submission
        join fetch submission.assignment assignment
        where submission.studentId in :studentIds
          and assignment.course.id = :courseId
          and assignment.deletedAt is null
          and assignment.status <> :excludedStatus
        """)
    List<AssignmentSubmission> findCourseSubmissionsForStudents(
            Collection<UUID> studentIds,
            UUID courseId,
            AssignmentStatus excludedStatus
    );
}
