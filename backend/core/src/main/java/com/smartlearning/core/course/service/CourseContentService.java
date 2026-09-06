package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.CourseChapterCreateRequest;
import com.smartlearning.core.course.dto.request.CourseChapterUpdateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicCreateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicUpdateRequest;
import com.smartlearning.core.course.dto.response.CourseChapterResponse;
import com.smartlearning.core.course.dto.response.CourseTopicResponse;

import java.util.List;
import java.util.UUID;

public interface CourseContentService {
    List<CourseChapterResponse> getChapters(UUID courseId, UUID currentUserId);

    CourseChapterResponse createChapter(UUID courseId, CourseChapterCreateRequest request, UUID currentUserId);

    CourseChapterResponse updateChapter(
            UUID courseId,
            UUID chapterId,
            CourseChapterUpdateRequest request,
            UUID currentUserId
    );

    void deleteChapter(UUID courseId, UUID chapterId, UUID currentUserId);

    List<CourseTopicResponse> getTopics(UUID courseId, UUID chapterId, UUID currentUserId);

    CourseTopicResponse createTopic(
            UUID courseId,
            UUID chapterId,
            CourseTopicCreateRequest request,
            UUID currentUserId
    );

    CourseTopicResponse updateTopic(
            UUID courseId,
            UUID chapterId,
            UUID topicId,
            CourseTopicUpdateRequest request,
            UUID currentUserId
    );

    void deleteTopic(UUID courseId, UUID chapterId, UUID topicId, UUID currentUserId);
}
