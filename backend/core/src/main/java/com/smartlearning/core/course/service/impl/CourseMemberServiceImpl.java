package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.request.JoinCourseRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.entity.AccessCode;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.mapper.CourseMemberMapper;
import com.smartlearning.core.course.repository.AccessCodeRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.CourseMemberService;
import com.smartlearning.core.course.utils.CourseUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final AccessCodeRepository accessCodeRepository;
    private final PasswordEncoder passwordEncoder;
    private final CourseAccessPolicy courseAccessPolicy;

    @Override
    public CourseMemberResponse addMember(UUID courseId, CourseMemberCreateRequest request, UUID currentUserId) {
        Course course = courseUtils.requireCourse(courseId);
        courseAccessPolicy.requireOwner(courseId, currentUserId);

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
    public CourseMemberResponse joinByCode(JoinCourseRequest request, UUID currentUserId) {
        Course course = courseUtils.requireCourse(request.courseId());

        CourseMember existing = memberRepository.findByCourseIdAndUserId(
                        request.courseId(),
                        currentUserId).orElse(null);

        if (existing != null && existing.getStatus() == CourseMemberStatus.ACTIVE) {
            return memberMapper.toResponse(existing);
        }

        List<AccessCode> activeCodes = accessCodeRepository
                .findAllByCourseIdAndActiveTrue(request.courseId());

        AccessCode matchedCode = activeCodes
                .stream()
                .filter(code -> passwordEncoder.matches(
                        request.code(),
                        code.getCodeHash()
                ))
                .findFirst()
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.DATA_CONFLICT,
                        "Invalid Access Code"
                ));

        Instant now = Instant.now();

        if (matchedCode.getExpiresAt() != null
                && matchedCode.getExpiresAt().isBefore(now)) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Access code hết hạn!"
            );
        }

//        if (matchedCode.getMaxUses() != null
//                && matchedCode.getUsedCount()
//                >= matchedCode.getMaxUses()) {
//            throw new ApplicationException(
//                    CommonErrorCode.DATA_CONFLICT,
//                    "Access Code đã đạt giới hạn sử dụng"
//            );
//        }

        CourseMember member;

        if (existing == null) {
            member = new CourseMember();
            member.setCourse(course);
            member.setUserId(currentUserId);
            member.setRole(
                    CourseMemberRole.STUDENT
            );
        } else {
            member = existing;
        }

        member.setStatus(CourseMemberStatus.ACTIVE);
        member.setJoinedAt(now);
        member.setRemovedAt(null);

//        matchedCode.setUsedCount(
//                matchedCode.getUsedCount() + 1
//        );

        return memberMapper.toResponse(
                memberRepository.save(member)
        );
    }
}
