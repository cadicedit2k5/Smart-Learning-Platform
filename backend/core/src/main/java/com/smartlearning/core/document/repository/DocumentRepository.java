package com.smartlearning.core.document.repository;

import com.smartlearning.core.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {

    @EntityGraph(attributePaths = {"version"})
    Page<Document> findAll(Specification<Document> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"version"})
    Optional<Document> findByIdAndCourseIdAndDeletedAtIsNull(UUID id, UUID courseId);

    @EntityGraph(attributePaths = {"version"})
    @Query("""
        SELECT document
        FROM Document document
        LEFT JOIN document.chapter chapter
        LEFT JOIN document.topic topic
        WHERE document.course.id = :courseId
          AND (:chapterId IS NULL OR chapter.id = :chapterId)
          AND (:topicId IS NULL OR topic.id = :topicId)
        """)
    List<Document> findAllByDeletionScope(
            @Param("courseId") UUID courseId,
            @Param("chapterId") UUID chapterId,
            @Param("topicId") UUID topicId
    );
}
