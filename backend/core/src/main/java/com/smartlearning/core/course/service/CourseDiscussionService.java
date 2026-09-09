package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.DiscussionCreateRequest;
import com.smartlearning.core.course.dto.request.DiscussionReplyCreateRequest;
import com.smartlearning.core.course.dto.response.DiscussionResponse;

import java.util.List;
import java.util.UUID;

public interface CourseDiscussionService {

    List<DiscussionResponse> getDiscussions(
            UUID courseId,
            UUID userId,
            String accessToken
    );

    DiscussionResponse createDiscussion(
            UUID courseId,
            UUID userId,
            String accessToken,
            DiscussionCreateRequest request
    );

    DiscussionResponse createReply(
            UUID courseId,
            UUID discussionId,
            UUID userId,
            String accessToken,
            DiscussionReplyCreateRequest request
    );
}