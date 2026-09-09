package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;
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
        name = "assignment_submission",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_assignment_submission_student",
                        columnNames = {
                                "assignment_id",
                                "student_id"
                        }
                )
        }
)
public class AssignmentSubmission extends BaseEntity {

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "assignment_id",
            nullable = false
    )
    private Assignment assignment;

    @Column(
            name = "student_id",
            nullable = false
    )
    private UUID studentId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_object_name")
    private String fileObjectName;

    @Column(name = "original_file_name")
    private String originalFileName;

    @Column(name = "file_content_type")
    private String fileContentType;

    @Column(name = "file_size")
    private Long fileSize;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private AssignmentSubmissionStatus status;

    @Column(
            name = "submitted_at",
            nullable = false
    )
    private Instant submittedAt;

    @Column(
            precision = 8,
            scale = 2
    )
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(name = "graded_at")
    private Instant gradedAt;

    @Column(name = "graded_by")
    private UUID gradedBy;
}
