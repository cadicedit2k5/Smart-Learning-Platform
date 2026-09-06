package com.smartlearning.core.document.repository;

import com.smartlearning.core.document.entity.DocumentProcessingJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentProcessingJobRepository extends JpaRepository<DocumentProcessingJob, UUID> {
}
