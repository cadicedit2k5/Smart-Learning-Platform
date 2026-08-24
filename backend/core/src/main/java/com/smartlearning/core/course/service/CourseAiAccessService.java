package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.response.CourseAiAccessResponse;

import java.util.UUID;

public interface CourseAiAccessService {
    CourseAiAccessResponse getAccess(
            UUID courseId,
            UUID userId
    );
}
