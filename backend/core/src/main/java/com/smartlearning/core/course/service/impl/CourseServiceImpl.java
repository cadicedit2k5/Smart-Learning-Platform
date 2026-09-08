package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.request.MyCourseFilterRequest;
import com.smartlearning.core.course.dto.request.PublicCourseFilterRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.repository.specification.CourseSpecifications;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
            course.setVisibility(CourseVisibility.INVITE_ONLY);
        }

        Course savedCourse = courseRepository.save(course);

        CourseMember owner = new CourseMember();
        owner.setCourse(savedCourse);
        owner.setUserId(currentUserId);
        owner.setRole(CourseMemberRole.OWNER);
        owner.setStatus(CourseMemberStatus.ACTIVE);
        owner.setJoinedAt(Instant.now());

        memberRepository.save(owner);

        return courseUtils.withRole(courseMapper.toResponse(savedCourse), CourseMemberRole.OWNER);
    }

    public CourseResponse getCourse(
            UUID courseId,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);

        CourseMember member;
        if (course.getVisibility() != CourseVisibility.PUBLIC
                || course.getStatus() != CourseStatus.PUBLISHED) {
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
        return courseUtils.withRole(courseMapper.toResponse(course), role);
    }

    @Override
    public PagingResponse<CourseResponse> getMyCourses(UUID currentUserId, MyCourseFilterRequest request) {

        Specification<Course> specifications = Specification.allOf(
                request.specification(),
                CourseSpecifications.members(currentUserId)
        );

        Page<CourseResponse> courses = courseRepository.findAll(specifications, request.pageable()).map(courseMapper::toResponse);

        return PagingResponse.from(courses);
    }

    @Override
    public PagingResponse<PublicCourseResponse> getPublicCourses(
            PublicCourseFilterRequest request,
            UUID currentUserId
    ) {
        var courses = courseRepository.findAll(request.specification(), request.pageable());

        List<UUID> courseIds = courses.getContent().stream()
                .map(Course::getId)
                .toList();
        Map<UUID, CourseMember> membershipsByCourseId = courseIds.isEmpty()
                ? Map.of()
                : memberRepository.findAllByCourseIdInAndUserId(courseIds, currentUserId)
                        .stream()
                        .collect(Collectors.toMap(
                                member -> member.getCourse().getId(),
                                Function.identity()
                        ));

        return PagingResponse.from(courses.map(course -> {
            CourseMember membership = membershipsByCourseId.get(course.getId());
            return new PublicCourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    course.getLevel(),
                    course.getPublishedAt(),
                    membership == null ? null : membership.getStatus()
            );
        }));
    }

    @Override
    public CourseResponse updateCourse(
            UUID courseId,
            CourseUpdateRequest request,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMember member = courseAccessPolicy.requireOwner(courseId, currentUserId);

        courseMapper.partialUpdate(request, course);

        if (course.getTitle() != null) {
            course.setTitle(course.getTitle().trim());
        }

        return courseUtils.withRole(
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

        return courseUtils.withRole(
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
}
