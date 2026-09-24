package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.Assignment;
import com.smartlearning.core.course.entity.enums.AssignmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    @Query("""
            SELECT a
            FROM Assignment a
            WHERE a.course.id = :courseId
              AND a.deletedAt IS NULL
            ORDER BY
                CASE WHEN a.dueAt >= CURRENT_TIMESTAMP THEN 0 ELSE 1 END ASC,
                CASE WHEN a.dueAt >= CURRENT_TIMESTAMP THEN a.dueAt END ASC,
                CASE WHEN a.dueAt < CURRENT_TIMESTAMP THEN a.dueAt END DESC
            """)
    List<Assignment> findAllByCourseIdOrdered(@Param("courseId") UUID courseId);

    Optional<Assignment> findByIdAndCourseIdAndDeletedAtIsNull(
            UUID assignmentId,
            UUID courseId
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Assignment a
        SET a.status = :closedStatus, a.closedAt = a.dueAt
        WHERE a.status = :publishedStatus
          AND a.dueAt <= :now
          AND a.deletedAt IS NULL
        """)
    int closeOverdueAssignments(
            @Param("publishedStatus") AssignmentStatus publishedStatus,
            @Param("closedStatus") AssignmentStatus closedStatus,
            @Param("now") Instant now
    );
}
