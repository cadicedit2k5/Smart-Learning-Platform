package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.response.StudentLearningProfileResponse;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.learningprofile.LearningProfileEvidence;
import com.smartlearning.core.course.learningprofile.LearningProfileEvidenceLoader;
import com.smartlearning.core.course.learningprofile.StudentLearningProfileCalculator;
import com.smartlearning.core.course.service.StudentLearningProfileService;
import com.smartlearning.core.course.utils.CourseUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StudentLearningProfileServiceImpl
        implements StudentLearningProfileService {

    private final CourseUtils courseUtils;
    private final LearningProfileEvidenceLoader evidenceLoader;
    private final StudentLearningProfileCalculator calculator;

    @Override
    public StudentLearningProfileResponse getStudentLearningProfile(
            UUID courseId,
            UUID studentId
    ) {
        courseUtils.requireCourse(courseId);

        LearningProfileEvidence evidence = evidenceLoader
                .load(courseId, List.of(studentId))
                .get(studentId);

        requireActiveStudent(evidence);

        return calculator.calculate(evidence);
    }

    private void requireActiveStudent(
            LearningProfileEvidence evidence
    ) {
        CourseMember member =
                evidence == null ? null : evidence.membership();

        if (
                member == null ||
                        member.getRole() != CourseMemberRole.STUDENT ||
                        member.getStatus() != CourseMemberStatus.ACTIVE
        ) {
            throw new ApplicationException(
                    CommonErrorCode.RESOURCE_NOT_FOUND,
                    "Không tìm thấy học viên trong khóa học."
            );
        }
    }
}