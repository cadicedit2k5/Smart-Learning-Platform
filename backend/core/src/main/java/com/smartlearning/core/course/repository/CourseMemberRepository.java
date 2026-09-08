package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseMemberRepository extends JpaRepository<CourseMember, UUID> {

    Optional<CourseMember> findByCourseIdAndUserId(UUID courseId, UUID userId);

    Page<CourseMember> findAllByCourseIdAndStatus(
            UUID courseId,
            CourseMemberStatus status,
            Pageable pageable
    );

    List<CourseMember> findAllByCourseIdInAndUserId(
            List<UUID> courseIds,
            UUID userId
    );

    List<CourseMember> findAllByUserIdAndStatusAndCourseDeletedAtIsNullOrderByCourseUpdatedAtDesc(
            UUID userId,
            CourseMemberStatus status
    );

}
