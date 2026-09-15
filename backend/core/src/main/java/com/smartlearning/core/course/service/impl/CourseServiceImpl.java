package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseCreateRequest;
import com.smartlearning.core.course.dto.request.CourseUpdateRequest;
import com.smartlearning.core.course.dto.request.MyCourseFilterRequest;
import com.smartlearning.core.course.dto.request.PublicCourseFilterRequest;
import com.smartlearning.core.course.dto.response.CourseResponse;
import com.smartlearning.core.course.dto.response.PublicCourseDetailResponse;
import com.smartlearning.core.course.dto.response.PublicCourseDetailResponse.ChapterOutline;
import com.smartlearning.core.course.dto.response.PublicCourseDetailResponse.TopicOutline;
import com.smartlearning.core.course.dto.response.PublicCourseResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMapper;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.repository.specification.CourseSpecifications;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseService;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.dto.StoredFile;
import com.smartlearning.storage.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    private final CourseMapper courseMapper;
    private final CourseRepository courseRepository;
    private final CourseMemberRepository memberRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseUtils courseUtils;
    private final FileStorageService fileStorageService;

    @Override
    public CourseResponse createCourse(CourseCreateRequest request, UUID currentUserId) {
        Course course = courseMapper.toEntity(request);
        course.setCreatedBy(currentUserId);
        course.setStatus(CourseStatus.DRAFT);

        if (course.getVisibility() == null) {
            course.setVisibility(CourseVisibility.INVITE_ONLY);
        }

        Course savedCourse = courseRepository.save(course);
        replaceCourseImage(savedCourse, request.image());

        CourseMember owner = new CourseMember();
        owner.setCourse(savedCourse);
        owner.setUserId(currentUserId);
        owner.setRole(CourseMemberRole.OWNER);
        owner.setStatus(CourseMemberStatus.ACTIVE);
        owner.setJoinedAt(Instant.now());
        memberRepository.save(owner);

        return courseUtils.withRole(courseMapper.toResponse(savedCourse), CourseMemberRole.OWNER);
    }

    @Override
    public CourseResponse getCourse(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMemberRole role = resolveCurrentUserRole(course, currentUserId);
        return courseUtils.withRole(courseMapper.toResponse(course), role);
    }

    @Override
    public PagingResponse<CourseResponse> getMyCourses(UUID currentUserId, MyCourseFilterRequest request) {
        Specification<Course> specification = Specification.allOf(
                request.specification(),
                CourseSpecifications.activeMembers(currentUserId)
        );

        Page<CourseResponse> courses = courseRepository.findAll(specification, request.pageable())
                .map(courseMapper::toResponse);

        return PagingResponse.from(courses);
    }

    @Override
    public PagingResponse<PublicCourseResponse> getPublicCourses(
            PublicCourseFilterRequest request,
            UUID currentUserId
    ) {
        Pageable pageable = request.pageable();

        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "publishedAt")
            );
        }

        Page<Course> courses = courseRepository.findAll(request.specification(), pageable);
        List<UUID> courseIds = courses.getContent().stream().map(Course::getId).toList();

        Map<UUID, CourseMember> membershipsByCourseId = courseIds.isEmpty()
                ? Map.of()
                : memberRepository.findAllByCourseIdInAndUserId(courseIds, currentUserId).stream()
                .collect(Collectors.toMap(member -> member.getCourse().getId(), Function.identity()));

        return PagingResponse.from(courses.map(course -> {
            CourseMember membership = membershipsByCourseId.get(course.getId());

            return new PublicCourseResponse(
                    course.getId(),
                    course.getTitle(),
                    course.getDescription(),
                    courseMapper.toImageUrl(course),
                    course.getLevel(),
                    course.getPublishedAt(),
                    membership == null ? null : membership.getStatus()
            );
        }));
    }

    @Override
    public PublicCourseDetailResponse getPublicCourseDetail(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        requirePublicPublishedCourse(course);

        CourseMemberStatus membershipStatus = memberRepository.findByCourseIdAndUserId(courseId, currentUserId)
                .map(CourseMember::getStatus)
                .orElse(null);

        List<CourseTopic> topics = topicRepository.findAllActiveByCourseIdOrderByPosition(courseId);

        Map<UUID, List<CourseTopic>> topicsByChapter = topics.stream()
                .collect(Collectors.groupingBy(
                        topic -> topic.getChapter().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<ChapterOutline> chapters = chapterRepository
                .findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(courseId)
                .stream()
                .map(chapter -> {
                    List<TopicOutline> topicOutlines = topicsByChapter
                            .getOrDefault(chapter.getId(), List.of())
                            .stream()
                            .map(topic -> new TopicOutline(
                                    topic.getId(),
                                    topic.getTitle(),
                                    topic.getDescription(),
                                    topic.getOrderIndex(),
                                    topic.getEstimatedMinutes()
                            ))
                            .toList();

                    return new ChapterOutline(
                            chapter.getId(),
                            chapter.getTitle(),
                            chapter.getDescription(),
                            chapter.getLearningObjectives(),
                            chapter.getOrderIndex(),
                            topicOutlines
                    );
                })
                .toList();

        return new PublicCourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                courseMapper.toImageUrl(course),
                course.getLevel(),
                course.getPublishedAt(),
                membershipStatus,
                chapters
        );
    }

    @Override
    public CourseResponse updateCourse(UUID courseId, CourseUpdateRequest request, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMember owner = courseAccessPolicy.requireOwner(courseId, currentUserId);

        courseMapper.partialUpdate(request, course);

        if (course.getTitle() != null) {
            course.setTitle(course.getTitle().trim());
        }

        replaceCourseImage(course, request.image());
        return courseUtils.withRole(courseMapper.toResponse(course), owner.getRole());
    }

    @Override
    public StoredFile getCourseImage(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        resolveCurrentUserRole(course, currentUserId);

        if (course.getCoverUrl() == null || course.getCoverUrl().isBlank()) {
            throw new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND, "Khóa học chưa có ảnh");
        }

        return fileStorageService.download(course.getCoverUrl());
    }

    @Override
    public CourseResponse publishCourse(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        CourseMember owner = courseAccessPolicy.requireOwner(courseId, currentUserId);

        if (course.getStatus() == CourseStatus.PUBLISHED) {
            throw new ApplicationException(CommonErrorCode.DATA_CONFLICT, "Khóa học đã được đăng tải công khai!");
        }

        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(Instant.now());

        return courseUtils.withRole(courseMapper.toResponse(course), owner.getRole());
    }

    @Override
    public void deleteCourse(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        course.setDeletedAt(Instant.now());
    }

    private void requirePublicPublishedCourse(Course course) {
        if (course.getVisibility() != CourseVisibility.PUBLIC || course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ApplicationException(
                    CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Khóa học không tồn tại hoặc chưa được công khai"
            );
        }
    }

    private CourseMemberRole resolveCurrentUserRole(Course course, UUID currentUserId) {
        if (course.getVisibility() == CourseVisibility.PUBLIC && course.getStatus() == CourseStatus.PUBLISHED) {
            return memberRepository.findByCourseIdAndUserId(course.getId(), currentUserId)
                    .filter(member -> member.getStatus() == CourseMemberStatus.ACTIVE)
                    .map(CourseMember::getRole)
                    .orElse(null);
        }

        return courseAccessPolicy.requireActiveMember(course.getId(), currentUserId).getRole();
    }

    private void replaceCourseImage(Course course, MultipartFile image) {
        if (image == null) return;

        validateCourseImage(image);

        FileUploadResponse uploaded = fileStorageService.upload(
                image,
                "core/courses/" + course.getId() + "/image"
        );

        String previousObjectName = course.getCoverUrl();
        course.setCoverUrl(uploaded.objectName());
        registerImageCleanup(previousObjectName, uploaded.objectName());
    }

    private void validateCourseImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new ApplicationException(CommonErrorCode.VALIDATION_FAILED, "Ảnh khóa học không được để trống");
        }

        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new ApplicationException(CommonErrorCode.VALIDATION_FAILED, "Ảnh khóa học không được vượt quá 5 MB");
        }

        String contentType = image.getContentType();

        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new ApplicationException(
                    CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                    "Ảnh khóa học chỉ hỗ trợ JPG, PNG hoặc WebP"
            );
        }
    }

    private void registerImageCleanup(String previousObjectName, String newObjectName) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteImageQuietly(previousObjectName);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteImageQuietly(previousObjectName);
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) {
                    deleteImageQuietly(newObjectName);
                }
            }
        });
    }

    private void deleteImageQuietly(String objectName) {
        if (objectName == null || objectName.isBlank()) return;

        try {
            fileStorageService.delete(objectName);
        } catch (RuntimeException exception) {
            log.warn("Không thể xóa course image: {}", objectName, exception);
        }
    }
}