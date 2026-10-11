package com.smartlearning.core.course.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "attempt_answer", schema = "core",
        uniqueConstraints = @UniqueConstraint(name = "uk_attempt_answer_question",
                columnNames = {"attempt_id", "question_id"}),
        indexes = @Index(name = "idx_attempt_answer_attempt", columnList = "attempt_id"))
public class AttemptAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", nullable = false, columnDefinition = "jsonb")
    private List<UUID> selectedOptionIds = new ArrayList<>();

    @Column(name = "awarded_score", nullable = false)
    private int awardedScore;
}