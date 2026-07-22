package com.smartlearning.core.course.entity;

import com.smartlearning.common.entity.BaseEntity;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(
        name = "course_member",
        schema = "core",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_member_course_user",
                        columnNames = {"course_id", "user_id"}
                )
        }
)
public class CourseMember extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private CourseMemberRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CourseMemberStatus status = CourseMemberStatus.PENDING;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "removed_at")
    private Instant removedAt;
}