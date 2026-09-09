package com.smartlearning.core.notification.service;

import com.smartlearning.core.notification.dto.response.NotificationResponse;
import com.smartlearning.core.notification.entity.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    List<NotificationResponse> getMyNotifications(UUID userId);

    long getUnreadCount(UUID userId);

    void markAsRead(UUID notificationId, UUID userId);

    void markAllAsRead(UUID userId);

    void create(
            UUID userId,
            NotificationType type,
            String title,
            String message,
            UUID courseId
    );

    void createForCourseStudents(
            UUID courseId,
            NotificationType type,
            String title,
            String message
    );
}