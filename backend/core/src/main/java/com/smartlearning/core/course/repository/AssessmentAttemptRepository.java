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
}