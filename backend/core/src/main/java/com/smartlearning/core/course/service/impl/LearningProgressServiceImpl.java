package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.response.CourseLearningProgressResponse;
import com.smartlearning.core.course.dto.response.TopicLearningProgressResponse;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.LearningProgress;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.repository.LearningProgressRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.LearningProgressService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class LearningProgressServiceImpl implements LearningProgressService {

    private final LearningProgressRepository progressRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseAccessPolicy courseAccessPolicy;

    @Override
    public TopicLearningProgressResponse startTopic(
            UUID courseId,
            UUID topicId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        CourseTopic topic = requireTopic(
                courseId,
                topicId
        );

        Instant now = Instant.now();

        LearningProgress progress = progressRepository
                .findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> {
                    LearningProgress created =
                            new LearningProgress();

                    created.setUserId(userId);
                    created.setTopic(topic);
                    created.setStatus(
                            LearningProgressStatus.IN_PROGRESS
                    );
                    created.setStartedAt(now);

                    return created;
                });

        /*
         * Không đổi COMPLETED trở lại IN_PROGRESS.
         */
        progress.setLastAccessedAt(now);

        LearningProgress saved =
                progressRepository.save(progress);

        return toResponse(saved);
    }

    @Override
    public TopicLearningProgressResponse completeTopic(
            UUID courseId,
            UUID topicId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        CourseTopic topic = requireTopic(
                courseId,
                topicId
        );

        Instant now = Instant.now();

        LearningProgress progress = progressRepository
                .findByUserIdAndTopicId(userId, topicId)
                .orElseGet(() -> {
                    LearningProgress created =
                            new LearningProgress();

                    created.setUserId(userId);
                    created.setTopic(topic);
                    created.setStartedAt(now);

                    return created;
                });

        progress.setStatus(
                LearningProgressStatus.COMPLETED
        );

        /*
         * Complete nhiều lần không làm thay đổi
         * thời điểm hoàn thành đầu tiên.
         */
        if (progress.getCompletedAt() == null) {
            progress.setCompletedAt(now);
        }

        progress.setLastAccessedAt(now);

        LearningProgress saved =
                progressRepository.save(progress);

        return toResponse(saved);
    }

    @Override
    public CourseLearningProgressResponse getMyCourseProgress(
            UUID courseId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        long totalTopics =
                topicRepository
                        .countByChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                                courseId
                        );

        long completedTopics =
                progressRepository.countCourseProgressByStatus(
                        userId,
                        courseId,
                        LearningProgressStatus.COMPLETED
                );

        int percentage = totalTopics == 0
                ? 0
                : (int) Math.round(
                completedTopics * 100.0 / totalTopics
        );

        List<LearningProgress> progress =
                progressRepository.findCourseProgress(
                        userId,
                        courseId
                );

        UUID lastTopicId = progress.isEmpty()
                ? null
                : progress.getFirst()
                .getTopic()
                .getId();

        List<TopicLearningProgressResponse> topics =
                progress.stream()
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

    private CourseTopic requireTopic(
            UUID courseId,
            UUID topicId
    ) {
        return topicRepository
                .findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                        topicId,
                        courseId
                )
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND
                        )
                );
    }

    private void requireStudent(
            UUID courseId,
            UUID userId
    ) {
        CourseMember membership =
                courseAccessPolicy.requireActiveMember(
                        courseId,
                        userId
                );

        /*
         * Admin hoặc Lecturer không tạo learning progress.
         */
        if (membership == null
                || membership.getRole()
                != CourseMemberRole.STUDENT) {

            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN
            );
        }
    }

    private TopicLearningProgressResponse toResponse(
            LearningProgress progress
    ) {
        return new TopicLearningProgressResponse(
                progress.getTopic().getId(),
                progress.getStatus(),
                progress.getStartedAt(),
                progress.getCompletedAt(),
                progress.getLastAccessedAt()
        );
    }
}