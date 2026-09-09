package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseTopic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseTopicRepository extends JpaRepository<CourseTopic, UUID> {
    Optional<CourseTopic> findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
            UUID id,
            UUID courseId
    );

    Optional<CourseTopic> findByIdAndChapterIdAndDeletedAtIsNull(UUID id, UUID chapterId);

    List<CourseTopic> findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(UUID chapterId);

    boolean existsByChapterIdAndOrderIndex(UUID chapterId, Integer orderIndex);

    boolean existsByChapterIdAndOrderIndexAndIdNot(UUID chapterId, Integer orderIndex, UUID id);

    @Query("select coalesce(max(topic.orderIndex), -1) from CourseTopic topic where topic.chapter.id = :chapterId")
    Integer findMaxOrderIndex(UUID chapterId);

    List<CourseTopic> findAllByChapterIdOrderByOrderIndexAsc(UUID chapterId);

    long countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
            UUID courseId
    );
}
