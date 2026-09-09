package com.smartlearning.core.notification.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "notification", schema = "core")
public class Notification extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}