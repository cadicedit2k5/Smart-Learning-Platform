package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.AssessmentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAttemptRepository extends JpaRepository<AssessmentAttempt, UUID> {

    boolean existsByAssessmentId(UUID assessmentId);

    Optional<AssessmentAttempt> findByIdAndStudentId(UUID attemptId, UUID studentId);

    @Query("""
            select a from AssessmentAttempt a
            where a.assessment.id in :assessmentIds and a.studentId = :studentId
            order by a.submittedAt desc
            """)
    List<AssessmentAttempt> findHistory(Collection<UUID> assessmentIds, UUID studentId);

    @Query("""
            select distinct a.assessment.id from AssessmentAttempt a
            where a.assessment.id in :assessmentIds
            """)
    List<UUID> findAttemptedAssessmentIds(Collection<UUID> assessmentIds);

    @Query("""
        select attempt
        from AssessmentAttempt attempt
        where attempt.studentId in :studentIds
          and exists (
              select quiz.id
              from TopicQuiz quiz
              where quiz.id = attempt.assessment.id
                and quiz.topic.chapter.course.id = :courseId
                and quiz.topic.deletedAt is null
                and quiz.topic.chapter.deletedAt is null
          )
        """)
    List<AssessmentAttempt> findCourseAttemptsForStudents(
            Collection<UUID> studentIds,
            UUID courseId
    );
}