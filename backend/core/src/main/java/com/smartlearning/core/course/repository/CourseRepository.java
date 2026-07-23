package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID> {

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);
}
