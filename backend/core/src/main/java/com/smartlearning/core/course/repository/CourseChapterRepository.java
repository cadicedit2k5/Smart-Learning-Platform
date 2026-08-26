package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.enums.CourseContentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, UUID> {
    List<CourseChapter> findAllByCourseIdAndStatusAndDeletedAtIsNullOrderByOrderIndexAsc(
            UUID courseId,
            CourseContentStatus status
    );

    Optional<CourseChapter> findByIdAndCourseIdAndDeletedAtIsNull(UUID id, UUID courseId);

    List<CourseChapter> findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID courseId);

    boolean existsByCourseIdAndOrderIndex(UUID courseId, Integer orderIndex);

    boolean existsByCourseIdAndOrderIndexAndIdNot(UUID courseId, Integer orderIndex, UUID id);

    @Query("select coalesce(max(chapter.orderIndex), -1) from CourseChapter chapter where chapter.course.id = :courseId")
    Integer findMaxOrderIndex(UUID courseId);
}
