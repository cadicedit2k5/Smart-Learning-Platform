package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "course", schema = "core")
public class Course extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseVisibility visibility = CourseVisibility.INVITE_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseStatus status = CourseStatus.DRAFT;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
