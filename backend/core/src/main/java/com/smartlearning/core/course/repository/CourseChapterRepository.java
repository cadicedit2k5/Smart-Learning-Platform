package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.enums.CourseContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, UUID> {
    List<CourseChapter> findAllByCourseIdAndStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
            UUID courseId,
            CourseContentStatus status
    );
}
