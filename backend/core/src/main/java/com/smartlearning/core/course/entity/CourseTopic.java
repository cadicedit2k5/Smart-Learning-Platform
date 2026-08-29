package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(
        name = "course_topic",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_chapter_topic_order",
                        columnNames = {"chapter_id", "order_index"}
                )
        }
)
public class CourseTopic extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chapter_id", nullable = false)
    private CourseChapter chapter;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
