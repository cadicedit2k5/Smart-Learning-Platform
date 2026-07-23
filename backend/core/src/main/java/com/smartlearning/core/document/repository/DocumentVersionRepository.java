package com.smartlearning.core.document.repository;

import com.smartlearning.core.document.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {
}
