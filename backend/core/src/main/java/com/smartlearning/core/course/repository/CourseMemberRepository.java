package com.smartlearning.core.course.repository;

import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CourseMemberRepository extends JpaRepository<CourseMember, UUID> {

    Optional<CourseMember> findByCourseIdAndUserId(UUID courseId, UUID userId);

    List<CourseMember> findAllByCourseIdAndStatus(
            UUID courseId,
            CourseMemberStatus status
    );

    @Query("""
    select member.course
    from CourseMember member
    where member.userId = :userId
      and member.status = :status
      and member.course.deletedAt is null
    order by member.course.updatedAt desc
    """)
    List<Course> findCoursesByUserIdAndStatus(
            UUID userId,
            CourseMemberStatus status
    );
}
