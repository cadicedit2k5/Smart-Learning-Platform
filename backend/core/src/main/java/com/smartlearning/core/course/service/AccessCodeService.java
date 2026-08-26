package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.AccessCodeCreateRequest;
import com.smartlearning.core.course.dto.response.AccessCodeCreatedResponse;
import com.smartlearning.core.course.dto.response.AccessCodeResponse;

import java.util.List;
import java.util.UUID;

public interface AccessCodeService {
    AccessCodeCreatedResponse createCode(UUID courseId,
            AccessCodeCreateRequest request,
            UUID currentUserId);

    List<AccessCodeResponse> getCodes(UUID courseId, UUID currentUserId);

    void revokeCode(UUID courseId, UUID codeId, UUID currentUserId);
}
