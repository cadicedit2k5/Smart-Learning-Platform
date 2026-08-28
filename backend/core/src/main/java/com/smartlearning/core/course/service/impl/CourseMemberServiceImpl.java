package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.entity.enums.CourseVisibility;
import com.smartlearning.core.course.mapper.CourseMemberMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseMemberService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseMemberServiceImpl implements CourseMemberService {

    private final CourseMemberRepository memberRepository;
    private final CourseMemberMapper memberMapper;
    private final CourseUtils courseUtils;
    private final CourseAccessPolicy courseAccessPolicy;

    @Override
    public CourseMemberResponse addMember(UUID courseId, CourseMemberCreateRequest request, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        requirePublished(course);

        if (course.getVisibility() != CourseVisibility.INVITE_ONLY) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Chỉ có thể mời trực tiếp thành viên vào khóa học giới hạn theo lời mời"
            );
        }

        Optional<CourseMember> existing =
                memberRepository.findByCourseIdAndUserId(
                        courseId,
                        request.userId()
                );

        if (existing.isPresent()
                && existing.get().getStatus() == CourseMemberStatus.ACTIVE) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Người dùng đã là thành viên của khóa học!"
            );
        }

        CourseMember member;

        if (existing.isPresent()) {
            member = existing.get();
        } else {
            member = new CourseMember();
            member.setCourse(course);
            member.setUserId(request.userId());
        }
        member.setRole(CourseMemberRole.STUDENT);
        member.setStatus(CourseMemberStatus.ACTIVE);
        member.setJoinedAt(Instant.now());
        member.setRemovedAt(null);
        member.setInvitedBy(currentUserId);

        return memberMapper.toResponse(
                memberRepository.save(member)
        );
    }

    @Override
    public List<CourseMemberResponse> getMembers(UUID courseId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(
                courseId,
                currentUserId
        );

        return memberRepository
                .findAllByCourseIdAndStatus(
                        courseId,
                        CourseMemberStatus.ACTIVE
                )
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public CourseMemberResponse getCurrentMember(UUID courseId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        CourseMember member = courseAccessPolicy.requireActiveMember(courseId, currentUserId);

        if (member == null) {
            member = memberRepository.findByCourseIdAndUserId(courseId, currentUserId)
                    .filter(value -> value.getStatus() == CourseMemberStatus.ACTIVE)
                    .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
        }

        return memberMapper.toResponse(member);
    }

    @Override
    public void removeMember(
            UUID courseId,
            UUID memberId,
            UUID currentUserId
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

        CourseMember member = memberRepository
                .findById(memberId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy thành viên này"
                ));

        if (!member.getCourse().getId().equals(courseId)) {
            throw new ApplicationException(
                    CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy thành viên này"
            );
        }

        if (member.getRole() == CourseMemberRole.OWNER) {
            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN,
                    "Không thể xóa chủ sở hữu!"
            );
        }

        member.setStatus(CourseMemberStatus.REMOVED);
        member.setRemovedAt(Instant.now());
    }

    @Override
    public CourseMemberResponse requestToJoin(UUID courseId, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        requirePublished(course);

        if (course.getVisibility() != CourseVisibility.PUBLIC) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Khóa học này không nhận yêu cầu tham gia công khai"
            );
        }

        CourseMember member = memberRepository
                .findByCourseIdAndUserId(courseId, currentUserId)
                .orElse(null);

        if (member != null && member.getStatus() == CourseMemberStatus.ACTIVE) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Người dùng đã là thành viên của khóa học"
            );
        }

        if (member != null && member.getStatus() == CourseMemberStatus.PENDING) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Yêu cầu tham gia đang chờ phản hồi"
            );
        }

        if (member == null) {
            member = new CourseMember();
            member.setCourse(course);
            member.setUserId(currentUserId);
            member.setRole(CourseMemberRole.STUDENT);
        }

        member.setStatus(CourseMemberStatus.PENDING);
        member.setJoinedAt(null);
        member.setInvitedBy(null);
        member.setRemovedAt(null);

        return memberMapper.toResponse(memberRepository.save(member));
    }

    @Override
    public List<CourseMemberResponse> getJoinRequests(UUID courseId, UUID currentUserId) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

        return memberRepository
                .findAllByCourseIdAndStatus(courseId, CourseMemberStatus.PENDING)
                .stream()
                .map(memberMapper::toResponse)
                .toList();
    }

    @Override
    public CourseMemberResponse approveJoinRequest(
            UUID courseId,
            UUID memberId,
            UUID currentUserId
    ) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);
        requirePublished(course);

        if (course.getVisibility() != CourseVisibility.PUBLIC) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Khóa học này không nhận yêu cầu tham gia công khai"
            );
        }

        CourseMember member = requirePendingRequest(courseId, memberId);
        member.setStatus(CourseMemberStatus.ACTIVE);
        member.setJoinedAt(Instant.now());
        member.setRemovedAt(null);

        return memberMapper.toResponse(member);
    }

    @Override
    public CourseMemberResponse rejectJoinRequest(
            UUID courseId,
            UUID memberId,
            UUID currentUserId
    ) {
        courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

        CourseMember member = requirePendingRequest(courseId, memberId);
        member.setStatus(CourseMemberStatus.REJECTED);
        member.setJoinedAt(null);

        return memberMapper.toResponse(member);
    }

    private CourseMember requirePendingRequest(UUID courseId, UUID memberId) {
        CourseMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy yêu cầu tham gia"
                ));

        if (!member.getCourse().getId().equals(courseId)
                || member.getStatus() != CourseMemberStatus.PENDING
                || member.getRole() != CourseMemberRole.STUDENT) {
            throw new ApplicationException(
                    CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy yêu cầu tham gia"
            );
        }

        return member;
    }

    private void requirePublished(Course course) {
        if (course.getStatus() != CourseStatus.PUBLISHED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Khóa học chưa được xuất bản hoặc đã lưu trữ"
            );
        }
    }
}
