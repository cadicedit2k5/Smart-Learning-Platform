package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.LearningActivity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningActivityRepository extends JpaRepository<LearningActivity, Long> {
}