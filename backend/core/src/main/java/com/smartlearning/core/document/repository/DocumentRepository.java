package com.smartlearning.core.document.repository;

import com.smartlearning.core.document.entity.Document;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;
import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID>, JpaSpecificationExecutor<Document> {

    @EntityGraph(attributePaths = {"version", "course"})
    Page<Document> findAll(Specification<Document> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"version", "course"})
    Optional<Document> findByIdAndDeletedAtIsNull(UUID id);

    @EntityGraph(attributePaths = {"version", "course"})
    Optional<Document> findByIdAndCourseIdAndDeletedAtIsNull(UUID id, UUID courseId);
}
