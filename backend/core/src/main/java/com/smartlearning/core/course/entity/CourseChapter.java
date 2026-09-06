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
        name = "course_chapter",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_chapter_order",
                        columnNames = {"course_id", "order_index"}
                )
        }
)
public class CourseChapter extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "learning_objectives", columnDefinition = "TEXT")
    private String learningObjectives;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
