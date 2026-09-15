package com.smartlearning.core.course.dto.response;

import java.util.List;
import java.util.UUID;

public record CourseLearningProgressResponse(
        UUID courseId,
        long totalTopics,
        long completedTopics,
        int progressPercentage,
        UUID lastTopicId,
        List<TopicLearningProgressResponse> topics
) {
}
