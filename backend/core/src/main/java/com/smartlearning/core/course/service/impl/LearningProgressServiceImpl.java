package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.response.CourseLearningProgressResponse;
import com.smartlearning.core.course.dto.response.LecturerCourseProgressResponse;
import com.smartlearning.core.course.dto.response.StudentLearningProgressDetailResponse;
import com.smartlearning.core.course.dto.response.TopicLearningProgressResponse;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.LearningProgress;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.repository.LearningProgressRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.LearningProgressService;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.infrastructure.dto.SystemUserResponse;
import com.smartlearning.core.infrastructure.http.SystemClient;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {

    private static final long HEARTBEAT_SECONDS = 30;

    private final LearningProgressRepository progressRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseMemberRepository memberRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseUtils courseUtils;
    private final SystemClient systemClient;

    @Override
    public TopicLearningProgressResponse startTopicActivity(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);

        CourseTopic topic = requireTopic(courseId, topicId);
        Instant now = Instant.now();

        LearningProgress progress = progressRepository.findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> createProgress(userId, topic, now));

        progress.setLastAccessedAt(now);

        return toResponse(progressRepository.save(progress));
    }

    @Override
    public TopicLearningProgressResponse recordActivity(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);

        CourseTopic topic = requireTopic(courseId, topicId);
        Instant now = Instant.now();

        LearningProgress progress = progressRepository.findByUserIdAndTopicId(userId, topicId).orElse(null);

        if (progress == null) {
            progress = createProgress(userId, topic, now);
        } else {
            addActiveTime(progress, now);
            progress.setLastAccessedAt(now);
        }

        return toResponse(progressRepository.save(progress));
    }

    @Override
    public TopicLearningProgressResponse completeTopic(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);

        CourseTopic topic = requireTopic(courseId, topicId);
        Instant now = Instant.now();

        LearningProgress progress = progressRepository.findByUserIdAndTopicId(userId, topicId).orElse(null);

        if (progress == null) {
            progress = createProgress(userId, topic, now);
        } else if (progress.getStatus() == LearningProgressStatus.COMPLETED) {
            return toResponse(progress);
        } else {
            addActiveTime(progress, now);
        }

        progress.setStatus(LearningProgressStatus.COMPLETED);
        progress.setCompletedAt(now);
        progress.setLastAccessedAt(now);

        return toResponse(progressRepository.save(progress));
    }

    @Override
    public CourseLearningProgressResponse getMyCourseProgress(UUID courseId, UUID userId) {
        requireStudent(courseId, userId);

        long totalTopics = topicRepository.countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(courseId);
        long completedTopics = progressRepository.countCourseProgressByStatus(
                userId,
                courseId,
                LearningProgressStatus.COMPLETED
        );

        int percentage = calculatePercentage(completedTopics, totalTopics);
        List<LearningProgress> progress = progressRepository.findCourseProgress(userId, courseId);
        UUID lastTopicId = progress.isEmpty() ? null : progress.getFirst().getTopic().getId();

        List<TopicLearningProgressResponse> topics = progress.stream()
                .map(this::toResponse)
                .toList();

        return new CourseLearningProgressResponse(
                courseId,
                totalTopics,
                completedTopics,
                percentage,
                lastTopicId,
                topics
        );
    }

    @Override
    public LecturerCourseProgressResponse getCourseStudentProgress(
            UUID courseId,
            UUID lecturerId,
            String accessToken,
            PagingRequest pagingRequest
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, lecturerId);

        Page<CourseMember> studentPage = memberRepository.findAllByCourseIdAndRoleAndStatus(
                courseId,
                CourseMemberRole.STUDENT,
                CourseMemberStatus.ACTIVE,
                pagingRequest.pageable()
        );

        long totalStudents = studentPage.getTotalElements();
        long totalTopics = topicRepository.countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(courseId);

        List<LearningProgressRepository.StudentCompletionSummary> completionSummary =
                totalStudents == 0 || totalTopics == 0
                        ? List.of()
                        : progressRepository.findStudentCompletionSummary(
                        courseId,
                        LearningProgressStatus.COMPLETED,
                        CourseMemberRole.STUDENT,
                        CourseMemberStatus.ACTIVE
                );

        long totalCompletedTopics = completionSummary.stream()
                .mapToLong(LearningProgressRepository.StudentCompletionSummary::getCompletedTopics)
                .sum();

        int averageProgress = totalStudents == 0 || totalTopics == 0
                ? 0
                : (int) Math.round(totalCompletedTopics * 100.0 / (totalStudents * totalTopics));

        long completedStudents = totalTopics == 0
                ? 0
                : completionSummary.stream()
                .filter(summary -> summary.getCompletedTopics() == totalTopics)
                .count();

        List<UUID> studentIds = studentPage.getContent().stream()
                .map(CourseMember::getUserId)
                .toList();

        List<LearningProgress> pageProgress = studentIds.isEmpty()
                ? List.of()
                : progressRepository.findCourseProgressForUsers(studentIds, courseId);

        Map<UUID, List<LearningProgress>> progressByUser = pageProgress.stream()
                .collect(Collectors.groupingBy(LearningProgress::getUserId));

        Map<UUID, SystemUserResponse> users = studentIds.isEmpty()
                ? Map.of()
                : systemClient.lookupUsers(studentIds, accessToken).stream()
                .collect(Collectors.toMap(
                        SystemUserResponse::id,
                        Function.identity(),
                        (first, second) -> first
                ));

        Page<LecturerCourseProgressResponse.StudentSummary> summaries = studentPage.map(member -> {
            UUID studentId = member.getUserId();
            List<LearningProgress> studentProgress = progressByUser.getOrDefault(studentId, List.of());
            SystemUserResponse user = users.get(studentId);

            return buildStudentSummary(studentId, user, studentProgress, totalTopics);
        });

        return new LecturerCourseProgressResponse(
                courseId,
                totalStudents,
                totalTopics,
                averageProgress,
                completedStudents,
                PagingResponse.from(summaries)
        );
    }

    @Override
    public StudentLearningProgressDetailResponse getStudentProgress(
            UUID courseId,
            UUID studentId,
            UUID lecturerId,
            String accessToken
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, lecturerId);
        requireActiveStudent(courseId, studentId);

        List<CourseChapter> chapters =
                chapterRepository.findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(courseId);

        List<CourseTopic> topics =
                topicRepository.findAllActiveByCourseIdOrderByPosition(courseId);

        List<LearningProgress> studentProgress =
                progressRepository.findCourseProgress(studentId, courseId);

        Map<UUID, LearningProgress> progressByTopic = studentProgress.stream()
                .collect(Collectors.toMap(
                        progress -> progress.getTopic().getId(),
                        Function.identity(),
                        (first, second) -> first
                ));

        Map<UUID, List<CourseTopic>> topicsByChapter = topics.stream()
                .collect(Collectors.groupingBy(topic -> topic.getChapter().getId()));

        List<StudentLearningProgressDetailResponse.ChapterProgress> chapterResponses =
                chapters.stream()
                        .map(chapter -> buildChapterProgress(
                                chapter,
                                topicsByChapter.getOrDefault(chapter.getId(), List.of()),
                                progressByTopic
                        ))
                        .toList();

        long totalTopics = topics.size();

        long completedTopics = studentProgress.stream()
                .filter(progress -> progress.getStatus() == LearningProgressStatus.COMPLETED)
                .count();

        long inProgressTopics = studentProgress.stream()
                .filter(progress -> progress.getStatus() == LearningProgressStatus.IN_PROGRESS)
                .count();

        int percentage = calculatePercentage(completedTopics, totalTopics);

        SystemUserResponse user = systemClient.lookupUsers(List.of(studentId), accessToken)
                .stream()
                .findFirst()
                .orElse(null);

        return new StudentLearningProgressDetailResponse(
                studentId,
                displayName(user, studentId),
                user == null ? null : user.email(),
                totalTopics,
                completedTopics,
                inProgressTopics,
                percentage,
                chapterResponses
        );
    }

    private LearningProgress createProgress(UUID userId, CourseTopic topic, Instant now) {
        LearningProgress progress = new LearningProgress();
        progress.setUserId(userId);
        progress.setTopic(topic);
        progress.setStatus(LearningProgressStatus.IN_PROGRESS);
        progress.setActiveSeconds(0);
        progress.setStartedAt(now);
        progress.setLastAccessedAt(now);
        return progress;
    }

    private LecturerCourseProgressResponse.StudentSummary buildStudentSummary(
            UUID studentId,
            SystemUserResponse user,
            List<LearningProgress> progress,
            long totalTopics
    ) {
        long completedTopics = progress.stream()
                .filter(item -> item.getStatus() == LearningProgressStatus.COMPLETED)
                .count();

        long inProgressTopics = progress.stream()
                .filter(item -> item.getStatus() == LearningProgressStatus.IN_PROGRESS)
                .count();

        return new LecturerCourseProgressResponse.StudentSummary(
                studentId,
                displayName(user, studentId),
                user == null ? null : user.email(),
                totalTopics,
                completedTopics,
                inProgressTopics,
                calculatePercentage(completedTopics, totalTopics)
        );
    }

    private StudentLearningProgressDetailResponse.ChapterProgress buildChapterProgress(
            CourseChapter chapter,
            List<CourseTopic> topics,
            Map<UUID, LearningProgress> progressByTopic
    ) {
        List<StudentLearningProgressDetailResponse.TopicProgress> topicResponses = topics.stream()
                .map(topic -> {
                    LearningProgress progress = progressByTopic.get(topic.getId());

                    if (progress == null) {
                        return new StudentLearningProgressDetailResponse.TopicProgress(
                                topic.getId(),
                                topic.getTitle(),
                                topic.getOrderIndex(),
                                "NOT_STARTED",
                                0,
                                null,
                                null,
                                null
                        );
                    }

                    return new StudentLearningProgressDetailResponse.TopicProgress(
                            topic.getId(),
                            topic.getTitle(),
                            topic.getOrderIndex(),
                            progress.getStatus().name(),
                            progress.getActiveSeconds(),
                            progress.getStartedAt(),
                            progress.getCompletedAt(),
                            progress.getLastAccessedAt()
                    );
                })
                .toList();

        long completedTopics = topics.stream()
                .filter(topic -> {
                    LearningProgress progress = progressByTopic.get(topic.getId());
                    return progress != null && progress.getStatus() == LearningProgressStatus.COMPLETED;
                })
                .count();

        return new StudentLearningProgressDetailResponse.ChapterProgress(
                chapter.getId(),
                chapter.getTitle(),
                chapter.getOrderIndex(),
                topics.size(),
                completedTopics,
                calculatePercentage(completedTopics, topics.size()),
                topicResponses
        );
    }

    private void addActiveTime(LearningProgress progress, Instant now) {
        long elapsedSeconds = Math.max(0, Duration.between(progress.getLastAccessedAt(), now).getSeconds());
        progress.setActiveSeconds(progress.getActiveSeconds() + Math.min(elapsedSeconds, HEARTBEAT_SECONDS));
    }

    private CourseTopic requireTopic(UUID courseId, UUID topicId) {
        return topicRepository
                .findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(topicId, courseId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requireStudent(UUID courseId, UUID userId) {
        CourseMember membership = courseAccessPolicy.requireActiveMember(courseId, userId);

        if (membership == null || membership.getRole() != CourseMemberRole.STUDENT) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
    }

    private void requireActiveStudent(UUID courseId, UUID studentId) {
        CourseMember member = memberRepository.findByCourseIdAndUserId(courseId, studentId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy học viên trong khóa học."
                ));

        if (member.getRole() != CourseMemberRole.STUDENT || member.getStatus() != CourseMemberStatus.ACTIVE) {
            throw new ApplicationException(
                    CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy học viên trong khóa học."
            );
        }
    }

    private int calculatePercentage(long completedTopics, long totalTopics) {
        if (totalTopics == 0) return 0;
        return (int) Math.round(completedTopics * 100.0 / totalTopics);
    }

    private String displayName(SystemUserResponse user, UUID studentId) {
        if (user != null && user.fullName() != null && !user.fullName().isBlank()) return user.fullName();
        if (user != null && user.email() != null && !user.email().isBlank()) return user.email();

        return "Học viên " + studentId.toString().substring(0, 8);
    }

    private TopicLearningProgressResponse toResponse(LearningProgress progress) {
        return new TopicLearningProgressResponse(
                progress.getTopic().getId(),
                progress.getStatus(),
                progress.getActiveSeconds(),
                progress.getStartedAt(),
                progress.getCompletedAt(),
                progress.getLastAccessedAt()
        );
    }
}