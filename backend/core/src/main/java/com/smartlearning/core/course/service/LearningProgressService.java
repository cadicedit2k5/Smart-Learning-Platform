package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.response.CourseLearningProgressResponse;
import com.smartlearning.core.course.dto.response.LecturerCourseProgressResponse;
import com.smartlearning.core.course.dto.response.StudentLearningProgressDetailResponse;
import com.smartlearning.core.course.dto.response.TopicLearningProgressResponse;

import java.util.UUID;

public interface LearningProgressService {

    TopicLearningProgressResponse startTopic(UUID courseId, UUID topicId, UUID userId);

    TopicLearningProgressResponse completeTopic(UUID courseId, UUID topicId, UUID userId);

    CourseLearningProgressResponse getMyCourseProgress(UUID courseId, UUID userId);

    LecturerCourseProgressResponse getCourseStudentProgress(
            UUID courseId,
            UUID lecturerId,
            String accessToken
    );

    StudentLearningProgressDetailResponse getStudentProgress(
            UUID courseId,
            UUID studentId,
            UUID lecturerId,
            String accessToken
    );
}