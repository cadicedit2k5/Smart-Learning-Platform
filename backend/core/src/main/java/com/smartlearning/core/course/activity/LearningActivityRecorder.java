package com.smartlearning.core.course.activity;

import com.smartlearning.core.course.entity.LearningActivity;
import com.smartlearning.core.course.entity.enums.LearningActivityType;
import com.smartlearning.core.course.repository.LearningActivityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LearningActivityRecorder {

    private final LearningActivityRepository activityRepository;
    private final JsonMapper jsonMapper;

    public void recordTopicStarted(UUID userId, UUID courseId, UUID topicId) {
        record(
                userId,
                courseId,
                LearningActivityType.TOPIC_STARTED,
                new LearningActivityData.TopicStartedData(topicId)
        );
    }

    public void recordTopicCompleted(UUID userId, UUID courseId, UUID topicId, long activeSeconds) {
        record(
                userId,
                courseId,
                LearningActivityType.TOPIC_COMPLETED,
                new LearningActivityData.TopicCompletedData(topicId, activeSeconds)
        );
    }

    public void recordAssignmentSubmitted(UUID userId, UUID courseId, UUID assignmentId, UUID submissionId) {
        record(
                userId,
                courseId,
                LearningActivityType.ASSIGNMENT_SUBMITTED,
                new LearningActivityData.AssignmentSubmittedData(assignmentId, submissionId)
        );
    }

    public void recordAssignmentGraded(
            UUID userId,
            UUID courseId,
            UUID assignmentId,
            UUID submissionId,
            BigDecimal score,
            BigDecimal maxScore
    ) {
        record(
                userId,
                courseId,
                LearningActivityType.ASSIGNMENT_GRADED,
                new LearningActivityData.AssignmentGradedData(assignmentId, submissionId, score, maxScore)
        );
    }

    private void record(
            UUID userId,
            UUID courseId,
            LearningActivityType eventType,
            LearningActivityData data
    ) {
        LearningActivity activity = new LearningActivity();
        activity.setUserId(userId);
        activity.setCourseId(courseId);
        activity.setEventType(eventType);
        activity.setOccurredAt(Instant.now());
        activity.setData(toMap(data));

        activityRepository.save(activity);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(LearningActivityData data) {
        return jsonMapper.convertValue(data, Map.class);
    }
}