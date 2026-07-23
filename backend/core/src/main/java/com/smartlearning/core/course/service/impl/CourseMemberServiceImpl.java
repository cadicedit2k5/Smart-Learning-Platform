package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.mapper.CourseMemberMapper;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.service.CourseMemberService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseMemberServiceImpl implements CourseMemberService {

    private final CourseMemberRepository memberRepository;
    private final CourseMemberMapper memberMapper;
    private final CourseUtils courseUtils;

    @Override
    public CourseMemberResponse addMember(UUID courseId, CourseMemberCreateRequest request, UUID currentUserId) {
//        permissionService.requireOwner(
//                courseId,
//                currentUserId
//        );

        if (request.role() == CourseMemberRole.OWNER) {
            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN,
                    "Không thể thêm OWNER bằng chức năng mời thành viên"
            );
        }

        Course course = courseUtils.requireCourse(courseId);

        Optional<CourseMember> existing =
                memberRepository.findByCourseIdAndUserId(
                        courseId,
                        request.userId()
                );

        if (existing.isPresent()
                && existing.get().getStatus()
                != CourseMemberStatus.REMOVED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Người dùng đã là thành viên của khóa học!"
            );
        }

        CourseMember member;

        if (existing.isPresent()) {
            member = existing.get();
            member.setRole(request.role());
            member.setStatus(CourseMemberStatus.PENDING);
            member.setInvitedBy(currentUserId);
            member.setJoinedAt(null);
            member.setRemovedAt(null);
        } else {
            member = new CourseMember();
            member.setCourse(course);
            member.setUserId(request.userId());
            member.setRole(request.role());
            member.setStatus(CourseMemberStatus.PENDING);
            member.setInvitedBy(currentUserId);
        }

        return memberMapper.toResponse(
                memberRepository.save(member)
        );
    }

    @Override
    public void removeMember(
            UUID courseId,
            UUID memberId,
            UUID currentUserId
    ) {
//        permissionService.requireOwner(
//                courseId,
//                currentUserId
//        );

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
}
