package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "course_announcement", schema = "core")
public class CourseAnnouncement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_id", nullable = false)
    private UUID authorId;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
