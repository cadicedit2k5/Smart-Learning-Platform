package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssignmentRepository extends JpaRepository<Assignment, UUID> {

    List<Assignment> findAllByCourseIdAndDeletedAtIsNullOrderByDueAtAsc(
            UUID courseId
    );

    Optional<Assignment> findByIdAndCourseIdAndDeletedAtIsNull(
            UUID assignmentId,
            UUID courseId
    );
}
