package com.smartlearning.core.notification.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.notification.dto.response.NotificationResponse;
import com.smartlearning.core.notification.entity.Notification;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import com.smartlearning.core.notification.repository.NotificationRepository;
import com.smartlearning.core.notification.service.NotificationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CourseMemberRepository courseMemberRepository;

    @Override
    public List<NotificationResponse> getMyNotifications(UUID userId) {
        return notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public long getUnreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Override
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository
                .findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND
                ));

        notification.setRead(true);
    }

    @Override
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications =
                notificationRepository.findAllByUserIdAndReadFalse(userId);

        notifications.forEach(notification -> notification.setRead(true));
    }

    @Override
    public void create(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID courseId
    ) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setCourseId(courseId);

        notificationRepository.save(notification);
    }

    @Override
    public void createForCourseStudents(
            UUID courseId,
            NotificationType type,
            String title,
            String message
    ) {
        List<CourseMember> students = courseMemberRepository
                .findAllByCourseIdAndRoleAndStatus(
                        courseId,
                        CourseMemberRole.STUDENT,
                        CourseMemberStatus.ACTIVE
                );

        List<Notification> notifications = students.stream()
                .map(member -> {
                    Notification notification = new Notification();
                    notification.setUserId(member.getUserId());
                    notification.setType(type);
                    notification.setTitle(title);
                    notification.setMessage(message);
                    notification.setCourseId(courseId);
                    return notification;
                })
                .toList();

        notificationRepository.saveAll(notifications);
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getCourseId(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
