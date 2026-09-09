package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.AnnouncementCreateRequest;
import com.smartlearning.core.course.dto.request.AnnouncementUpdateRequest;
import com.smartlearning.core.course.dto.response.AnnouncementResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseAnnouncement;
import com.smartlearning.core.course.repository.CourseAnnouncementRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseAnnouncementService;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import com.smartlearning.core.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseAnnouncementServiceImpl implements CourseAnnouncementService {

    private final CourseAnnouncementRepository announcementRepository;
    private final CourseRepository courseRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final NotificationService notificationService;

    @Override
    public List<AnnouncementResponse> getAnnouncements(UUID courseId, UUID userId) {
        courseAccessPolicy.requireActiveMember(courseId, userId);

        return announcementRepository
                .findAllByCourseIdAndDeletedAtIsNullOrderByCreatedAtDesc(courseId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AnnouncementResponse createAnnouncement(
            UUID courseId,
            UUID userId,
            AnnouncementCreateRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        CourseAnnouncement announcement = new CourseAnnouncement();
        announcement.setCourse(course);
        announcement.setAuthorId(userId);
        announcement.setTitle(request.title().trim());
        announcement.setContent(request.content().trim());

        CourseAnnouncement saved = announcementRepository.save(announcement);

        notificationService.createForCourseStudents(
                courseId,
                NotificationType.ANNOUNCEMENT_CREATED,
                "Thông báo mới",
                saved.getTitle()
        );

        return toResponse(saved);
    }

    @Override
    public AnnouncementResponse updateAnnouncement(
            UUID courseId,
            UUID announcementId,
            UUID userId,
            AnnouncementUpdateRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);

        CourseAnnouncement announcement = requireAnnouncement(courseId, announcementId);

        if (request.title() != null) {
            announcement.setTitle(request.title().trim());
        }

        if (request.content() != null) {
            announcement.setContent(request.content().trim());
        }

        return toResponse(announcementRepository.save(announcement));
    }

    @Override
    public void deleteAnnouncement(
            UUID courseId,
            UUID announcementId,
            UUID userId
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);

        CourseAnnouncement announcement = requireAnnouncement(courseId, announcementId);
        announcement.setDeletedAt(Instant.now());
    }

    private CourseAnnouncement requireAnnouncement(UUID courseId, UUID announcementId) {
        return announcementRepository
                .findByIdAndCourseIdAndDeletedAtIsNull(announcementId, courseId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private AnnouncementResponse toResponse(CourseAnnouncement announcement) {
        return new AnnouncementResponse(
                announcement.getId(),
                announcement.getCourse().getId(),
                announcement.getAuthorId(),
                announcement.getTitle(),
                announcement.getContent(),
                announcement.getCreatedAt(),
                announcement.getUpdatedAt()
        );
    }
}
