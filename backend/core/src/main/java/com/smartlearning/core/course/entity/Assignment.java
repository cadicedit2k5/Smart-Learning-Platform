package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.Course;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "assignment",
        schema = "core"
)
public class Assignment extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "course_id",
            nullable = false
    )
    private Course course;

    @Column(
            nullable = false,
            length = 255
    )
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(
            name = "due_at",
            nullable = false
    )
    private Instant dueAt;

    @Column(
            name = "max_score",
            nullable = false,
            precision = 8,
            scale = 2
    )
    private BigDecimal maxScore;

    @Column(
            name = "created_by",
            nullable = false
    )
    private UUID createdBy;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
