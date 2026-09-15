package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface CourseRepository extends JpaRepository<Course, UUID>, JpaSpecificationExecutor<Course> {

    Optional<Course> findByIdAndDeletedAtIsNull(UUID id);
}
