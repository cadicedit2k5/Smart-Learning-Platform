package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.LearningProgress;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LearningProgressRepository extends JpaRepository<LearningProgress, UUID> {

    interface StudentCompletionSummary {
        UUID getUserId();
        long getCompletedTopics();
    }

    Optional<LearningProgress> findByUserIdAndTopicId(UUID userId, UUID topicId);

    @Query("""
        select progress
        from LearningProgress progress
        where progress.userId = :userId
          and progress.topic.chapter.course.id = :courseId
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
        order by progress.lastAccessedAt desc
        """)
    List<LearningProgress> findCourseProgress(UUID userId, UUID courseId);

    @Query("""
        select progress
        from LearningProgress progress
        where progress.userId in :userIds
          and progress.topic.chapter.course.id = :courseId
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
        """)
    List<LearningProgress> findCourseProgressForUsers(Collection<UUID> userIds, UUID courseId);

    @Query("""
        select count(progress)
        from LearningProgress progress
        where progress.userId = :userId
          and progress.topic.chapter.course.id = :courseId
          and progress.status = :status
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
        """)
    long countCourseProgressByStatus(UUID userId, UUID courseId, LearningProgressStatus status);

    @Query("""
        select progress.userId as userId, count(progress.id) as completedTopics
        from LearningProgress progress
        where progress.topic.chapter.course.id = :courseId
          and progress.status = :progressStatus
          and progress.topic.deletedAt is null
          and progress.topic.chapter.deletedAt is null
          and exists (
              select member.id
              from CourseMember member
              where member.course.id = :courseId
                and member.userId = progress.userId
                and member.role = :role
                and member.status = :memberStatus
          )
        group by progress.userId
        """)
    List<StudentCompletionSummary> findStudentCompletionSummary(
            UUID courseId,
            LearningProgressStatus progressStatus,
            CourseMemberRole role,
            CourseMemberStatus memberStatus
    );
}