package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseDiscussion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseDiscussionRepository extends JpaRepository<CourseDiscussion, UUID> {

    List<CourseDiscussion> findAllByCourseIdOrderByCreatedAtDesc(UUID courseId);

    Optional<CourseDiscussion> findByIdAndCourseId(UUID discussionId, UUID courseId);
}