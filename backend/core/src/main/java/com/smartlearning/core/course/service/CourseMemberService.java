package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.request.JoinCourseRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;

import java.util.List;
import java.util.UUID;

public interface CourseMemberService {
    CourseMemberResponse addMember(UUID courseId,
            CourseMemberCreateRequest request,
            UUID currentUserId);

    List<CourseMemberResponse> getMembers(
            UUID courseId,
            UUID currentUserId
    );

    CourseMemberResponse getCurrentMember(UUID courseId, UUID currentUserId);

    void removeMember(UUID courseId, UUID memberId, UUID currentUserId);
    CourseMemberResponse joinByCode(JoinCourseRequest request, UUID currentUserId);
}
