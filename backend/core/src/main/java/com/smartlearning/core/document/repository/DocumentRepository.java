package com.smartlearning.core.document.repository;

import com.smartlearning.core.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
}
