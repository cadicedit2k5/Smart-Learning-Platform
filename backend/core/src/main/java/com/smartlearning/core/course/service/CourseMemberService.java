package com.smartlearning.core.course.service;

import com.smartlearning.common.dto.request.PagingRequest;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberDetailResponse;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;

import java.util.List;
import java.util.UUID;

public interface CourseMemberService {
    CourseMemberResponse addMember(UUID courseId,
            CourseMemberCreateRequest request,
            UUID currentUserId);

    PagingResponse<CourseMemberDetailResponse> getMembers(
            UUID courseId,
            UUID currentUserId,
            String accessToken,
            PagingRequest request
    );

    CourseMemberResponse getCurrentMember(UUID courseId, UUID currentUserId);

    void removeMember(UUID courseId, UUID memberId, UUID currentUserId);

    CourseMemberResponse requestToJoin(UUID courseId, UUID currentUserId);

    PagingResponse<CourseMemberDetailResponse> getJoinRequests(UUID courseId, UUID currentUserId, String accessToken, PagingRequest request);

    CourseMemberResponse approveJoinRequest(UUID courseId, UUID memberId, UUID currentUserId);

    CourseMemberResponse rejectJoinRequest(UUID courseId, UUID memberId, UUID currentUserId);
}
