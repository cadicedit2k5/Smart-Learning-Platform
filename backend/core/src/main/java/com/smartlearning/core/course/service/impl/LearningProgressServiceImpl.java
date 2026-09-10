package com.smartlearning.core.course.service.impl;

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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {

    private final LearningProgressRepository progressRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseChapterRepository chapterRepository;
    private final CourseMemberRepository memberRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final CourseUtils courseUtils;
    private final SystemClient systemClient;

    @Override
    public TopicLearningProgressResponse startTopic(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);
        CourseTopic topic = requireTopic(courseId, topicId);
        Instant now = Instant.now();

        LearningProgress progress = progressRepository.findByUserIdAndTopicId(userId, topicId).orElseGet(() -> {
            LearningProgress created = new LearningProgress();
            created.setUserId(userId);
            created.setTopic(topic);
            created.setStatus(LearningProgressStatus.IN_PROGRESS);
            created.setStartedAt(now);
            return created;
        });

        progress.setLastAccessedAt(now);
        LearningProgress saved = progressRepository.save(progress);
        return toResponse(saved);
    }

    @Override
    public TopicLearningProgressResponse completeTopic(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);
        CourseTopic topic = requireTopic(courseId, topicId);
        Instant now = Instant.now();

        LearningProgress progress = progressRepository.findByUserIdAndTopicId(userId, topicId).orElseGet(() -> {
            LearningProgress created = new LearningProgress();
            created.setUserId(userId);
            created.setTopic(topic);
            created.setStartedAt(now);
            return created;
        });

        progress.setStatus(LearningProgressStatus.COMPLETED);

        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(now);
        }

        progress.setLastAccessedAt(now);
        LearningProgress saved = progressRepository.save(progress);
        return toResponse(saved);
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
            String accessToken
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, lecturerId);

        List<CourseMember> students = memberRepository.findAllByCourseIdAndRoleAndStatus(
                courseId,
                CourseMemberRole.STUDENT,
                CourseMemberStatus.ACTIVE
        );

        long totalTopics = topicRepository
                .countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(courseId);

        if (students.isEmpty()) {
            return new LecturerCourseProgressResponse(
                    courseId,
                    0,
                    totalTopics,
                    0,
                    0,
                    List.of()
            );
        }

        List<UUID> studentIds = students.stream()
                .map(CourseMember::getUserId)
                .toList();

        List<LearningProgress> allProgress =
                progressRepository.findCourseProgressForUsers(studentIds, courseId);

        Map<UUID, List<LearningProgress>> progressByUser = allProgress.stream()
                .collect(Collectors.groupingBy(LearningProgress::getUserId));

        Map<UUID, SystemUserResponse> users = systemClient.lookupUsers(studentIds, accessToken).stream()
                .collect(Collectors.toMap(
                        SystemUserResponse::id,
                        Function.identity(),
                        (first, second) -> first
                ));

        List<LecturerCourseProgressResponse.StudentSummary> summaries = students.stream()
                .map(member -> {
                    UUID studentId = member.getUserId();
                    List<LearningProgress> studentProgress =
                            progressByUser.getOrDefault(studentId, List.of());

                    long completedTopics = studentProgress.stream()
                            .filter(progress -> progress.getStatus() == LearningProgressStatus.COMPLETED)
                            .count();

                    long inProgressTopics = studentProgress.stream()
                            .filter(progress -> progress.getStatus() == LearningProgressStatus.IN_PROGRESS)
                            .count();

                    int percentage = calculatePercentage(completedTopics, totalTopics);
                    SystemUserResponse user = users.get(studentId);

                    return new LecturerCourseProgressResponse.StudentSummary(
                            studentId,
                            displayName(user, studentId),
                            user == null ? null : user.email(),
                            totalTopics,
                            completedTopics,
                            inProgressTopics,
                            percentage
                    );
                })
                .sorted(Comparator.comparing(
                        LecturerCourseProgressResponse.StudentSummary::fullName,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();

        int averageProgress = summaries.isEmpty()
                ? 0
                : (int) Math.round(
                summaries.stream()
                        .mapToInt(LecturerCourseProgressResponse.StudentSummary::progressPercentage)
                        .average()
                        .orElse(0)
        );

        long completedStudents = totalTopics == 0
                ? 0
                : summaries.stream()
                .filter(student -> student.progressPercentage() == 100)
                .count();

        return new LecturerCourseProgressResponse(
                courseId,
                summaries.size(),
                totalTopics,
                averageProgress,
                completedStudents,
                summaries
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
        if (user != null && user.fullName() != null && !user.fullName().isBlank()) {
            return user.fullName();
        }

        if (user != null && user.email() != null && !user.email().isBlank()) {
            return user.email();
        }

        return "Học viên " + studentId.toString().substring(0, 8);
    }

    private TopicLearningProgressResponse toResponse(LearningProgress progress) {
        return new TopicLearningProgressResponse(
                progress.getTopic().getId(),
                progress.getStatus(),
                progress.getStartedAt(),
                progress.getCompletedAt(),
                progress.getLastAccessedAt()
        );
    }
}