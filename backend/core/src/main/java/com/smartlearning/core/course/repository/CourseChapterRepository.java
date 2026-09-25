package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseChapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseChapterRepository extends JpaRepository<CourseChapter, UUID> {

    Optional<CourseChapter> findByIdAndCourseIdAndDeletedAtIsNull(UUID id, UUID courseId);

    List<CourseChapter> findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID courseId);

    boolean existsByCourseIdAndOrderIndexAndDeletedAtIsNull(UUID courseId, Integer orderIndex);

    boolean existsByCourseIdAndOrderIndexAndIdNotAndDeletedAtIsNull(UUID courseId, Integer orderIndex, UUID id);

    @Query("""
            select coalesce(max(chapter.orderIndex), -1)
            from CourseChapter chapter
            where chapter.course.id = :courseId
              and chapter.deletedAt is null
            """)
    Integer findMaxOrderIndex(UUID courseId);
}