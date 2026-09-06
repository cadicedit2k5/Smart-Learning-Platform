package com.smartlearning.core.document.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "document", schema = "core")
public class Document extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id")
    private CourseChapter chapter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id")
    private CourseTopic topic;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "version_id",
            unique = true
    )
    private DocumentVersion version;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "uploaded_by", nullable = false)
    private UUID uploadedBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}