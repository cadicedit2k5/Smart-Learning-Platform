package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "learning_progress",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_learning_progress_user_topic",
                        columnNames = {"user_id", "topic_id"}
                )
        }
)
public class LearningProgress extends BaseEntity {

    @Column(
            name = "user_id",
            nullable = false
    )
    private UUID userId;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "topic_id",
            nullable = false
    )
    private CourseTopic topic;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 30
    )
    private LearningProgressStatus status;

    @Column(
            name = "started_at",
            nullable = false
    )
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(
            name = "last_accessed_at",
            nullable = false
    )
    private Instant lastAccessedAt;
}