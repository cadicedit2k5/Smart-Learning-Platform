package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseAnnouncement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CourseAnnouncementRepository extends JpaRepository<CourseAnnouncement, UUID> {

    Page<CourseAnnouncement> findAllByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID courseId,
            Pageable pageable
    );

    Optional<CourseAnnouncement> findByIdAndCourseIdAndDeletedAtIsNull(
            UUID announcementId,
            UUID courseId
    );
}