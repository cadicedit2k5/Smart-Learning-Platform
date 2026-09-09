package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.LearningProgress;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningProgressRepository
        extends JpaRepository<LearningProgress, UUID> {

    Optional<LearningProgress> findByUserIdAndTopicId(
            UUID userId,
            UUID topicId
    );

    @Query("""
        select progress
        from LearningProgress progress
        where progress.userId = :userId
          and progress.topic.chapter.course.id = :courseId
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
        order by progress.lastAccessedAt desc
        """)
    List<LearningProgress> findCourseProgress(
            UUID userId,
            UUID courseId
    );

    @Query("""
        select count(progress)
        from LearningProgress progress
        where progress.userId = :userId
          and progress.topic.chapter.course.id = :courseId
          and progress.status = :status
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
        """)
    long countCourseProgressByStatus(
            UUID userId,
            UUID courseId,
            LearningProgressStatus status
    );
}
