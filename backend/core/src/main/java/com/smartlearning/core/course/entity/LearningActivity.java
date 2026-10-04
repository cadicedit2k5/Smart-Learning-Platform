package com.smartlearning.core.course.entity;

import com.smartlearning.core.course.entity.enums.LearningActivityType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "learning_activity",
        schema = "core",
        indexes = {
                @Index(
                        name = "idx_learning_activity_user_course_time",
                        columnList = "user_id, course_id, occurred_at"
                ),
                @Index(
                        name = "idx_learning_activity_course_type_time",
                        columnList = "course_id, event_type, occurred_at"
                )
        }
)
public class LearningActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "course_id", nullable = false)
    private UUID courseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private LearningActivityType eventType;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> data;
}