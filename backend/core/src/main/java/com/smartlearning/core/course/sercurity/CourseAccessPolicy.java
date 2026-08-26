package com.smartlearning.core.course.sercurity;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CourseAccessPolicy {
    private final CourseMemberRepository memberRepository;
    private final CurrentUserAccess currentUserAccess;

    public CourseMember requireActiveMember(
            UUID courseId,
            UUID userId
    ) {
        if (currentUserAccess.isAdmin()) {
            return null;
        }

        return requireActiveMembership(courseId, userId);
    }

    public CourseMember requireOwner(
            UUID courseId,
            UUID userId
    ) {
        if (currentUserAccess.isAdmin()) {
            return null;
        }

        CourseMember member = requireActiveMembership(courseId, userId);

        if (member.getRole() != CourseMemberRole.OWNER) {
            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN
            );
        }

        return member;
    }

    private CourseMember requireActiveMembership(
            UUID courseId,
            UUID userId
    ) {
        CourseMember member = memberRepository
                .findByCourseIdAndUserId(courseId, userId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.FORBIDDEN
                ));

        if (member.getStatus() != CourseMemberStatus.ACTIVE) {
            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN
            );
        }

        return member;
    }
}
