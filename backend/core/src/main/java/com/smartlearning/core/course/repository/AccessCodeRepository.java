package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.AccessCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessCodeRepository extends JpaRepository<AccessCode, UUID> {
}
