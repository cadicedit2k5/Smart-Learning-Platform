package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseAnnouncement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {

    List<CourseAnnouncement> findAllByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID courseId);

    Optional<CourseAnnouncement> findByIdAndCourseIdAndDeletedAtIsNull(UUID announcementId, UUID courseId);
}