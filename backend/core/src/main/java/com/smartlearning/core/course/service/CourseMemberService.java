package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberDetailResponse;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;

import java.util.List;
import java.util.UUID;

public interface CourseMemberService {
    CourseMemberResponse addMember(UUID courseId,
            CourseMemberCreateRequest request,
            UUID currentUserId);

    List<CourseMemberDetailResponse> getMembers(
            UUID courseId,
            UUID currentUserId,
            String accessToken
    );

    CourseMemberResponse getCurrentMember(UUID courseId, UUID currentUserId);

    void removeMember(UUID courseId, UUID memberId, UUID currentUserId);

    CourseMemberResponse requestToJoin(UUID courseId, UUID currentUserId);

    List<CourseMemberDetailResponse> getJoinRequests(UUID courseId, UUID currentUserId, String accessToken);

    CourseMemberResponse approveJoinRequest(UUID courseId, UUID memberId, UUID currentUserId);

    CourseMemberResponse rejectJoinRequest(UUID courseId, UUID memberId, UUID currentUserId);
}
