package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;

import java.util.UUID;

public interface CourseMemberService {
    CourseMemberResponse addMember(UUID courseId,
            CourseMemberCreateRequest request,
            UUID currentUserId);

    void removeMember(UUID courseId, UUID memberId, UUID currentUserId);
}
