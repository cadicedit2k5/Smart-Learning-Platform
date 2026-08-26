package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;
    private final CourseMemberRepository memberRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseUtils courseUtils;

    @Override
    public CourseResponse createCourse(
            CourseCreateRequest request,
            UUID currentUserId
    ) {
        Course course = courseMapper.toEntity(request);

        course.setCreatedBy(currentUserId);
        course.setStatus(CourseStatus.DRAFT);

        if (course.getVisibility() == null) {
            course.setVisibility(CourseVisibility.PRIVATE);
        }

        Course savedCourse = courseRepository.save(course);

        CourseMember owner = new CourseMember();
        owner.setCourse(savedCourse);
        owner.setUserId(currentUserId);
        owner.setRole(CourseMemberRole.OWNER);
        owner.setStatus(CourseMemberStatus.ACTIVE);
        owner.setJoinedAt(Instant.now());

        memberRepository.save(owner);

        return withRole(courseMapper.toResponse(savedCourse), CourseMemberRole.OWNER);
    }

    public CourseResponse getCourse(
            UUID courseId,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);

        CourseMember member;
        if (course.getVisibility() != CourseVisibility.PUBLIC) {
            member = courseAccessPolicy.requireActiveMember(
                    courseId,
                    currentUserId
            );
        } else {
            member = memberRepository
                    .findByCourseIdAndUserId(courseId, currentUserId)
                    .filter(value -> value.getStatus() == CourseMemberStatus.ACTIVE)
                    .orElse(null);
        }

        CourseMemberRole role = member == null ? null : member.getRole();
        return withRole(courseMapper.toResponse(course), role);
    }

    @Override
    public List<CourseResponse> getMyCourses(UUID currentUserId) {
        return memberRepository
                .findAllByUserIdAndStatusAndCourseDeletedAtIsNullOrderByCourseUpdatedAtDesc(
                        currentUserId,
                        CourseMemberStatus.ACTIVE
                )
                .stream()
                .map(member -> withRole(
                        courseMapper.toResponse(member.getCourse()),
                        member.getRole()
                ))
                .toList();
    }

    @Override
    public CourseResponse updateCourse(
            UUID courseId,
            CourseUpdateRequest request,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMember member = courseAccessPolicy.requireTeachingMember(courseId, currentUserId);

        courseMapper.partialUpdate(request, course);

        if (course.getTitle() != null) {
            course.setTitle(course.getTitle().trim());
        }

        return withRole(
                courseMapper.toResponse(course),
                member == null ? null : member.getRole()
        );
    }

    @Override
    public CourseResponse publishCourse(
            UUID courseId,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMember owner = courseAccessPolicy.requireOwner(courseId, currentUserId);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Khóa học đã được đăng tải công khai!"
            );
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());

        return withRole(
                courseMapper.toResponse(course),
                owner == null ? null : owner.getRole()
        );
    }

    @Override
    public void deleteCourse(
            UUID courseId,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        course.setDeletedAt(Instant.now());
    }

    private CourseResponse withRole(CourseResponse response, CourseMemberRole role) {
        return new CourseResponse(
                response.id(),
                response.title(),
                response.description(),
                response.level(),
                response.visibility(),
                response.status(),
                response.createdBy(),
                response.publishedAt(),
                response.createdAt(),
                response.updatedAt(),
                role
        );
    }
}
