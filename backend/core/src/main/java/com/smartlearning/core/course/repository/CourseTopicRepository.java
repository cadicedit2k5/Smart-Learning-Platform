package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseTopic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CourseTopicRepository extends JpaRepository<CourseTopic, UUID> {
}
