package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.AssignmentCreateRequest;
import com.smartlearning.core.course.dto.request.AssignmentGradeRequest;
import com.smartlearning.core.course.dto.request.AssignmentSubmissionRequest;
import com.smartlearning.core.course.dto.request.AssignmentUpdateRequest;
import com.smartlearning.core.course.dto.response.AssignmentResponse;
import com.smartlearning.core.course.dto.response.AssignmentSubmissionResponse;
import com.smartlearning.core.course.entity.Assignment;
import com.smartlearning.core.course.entity.AssignmentSubmission;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.repository.AssignmentRepository;
import com.smartlearning.core.course.repository.AssignmentSubmissionRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.AssignmentService;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentServiceImpl
        implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final FileStorageService fileStorageService;

    @Override
    public List<AssignmentResponse> getAssignments(
            UUID courseId,
            UUID userId
    ) {
        courseAccessPolicy.requireActiveMember(
                courseId,
                userId
        );

        return assignmentRepository
                .findAllByCourseIdAndDeletedAtIsNullOrderByDueAtAsc(
                        courseId
                )
                .stream()
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public AssignmentResponse createAssignment(
            UUID courseId,
            UUID userId,
            AssignmentCreateRequest request
    ) {
        courseAccessPolicy.requireOwner(
                courseId,
                userId
        );

        Course course = courseRepository
                .findById(courseId)
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND
                        )
                );

        Assignment assignment = new Assignment();

        assignment.setCourse(course);
        assignment.setTitle(request.title().trim());
        assignment.setDescription(
                normalize(request.description())
        );
        assignment.setDueAt(request.dueAt());
        assignment.setMaxScore(request.maxScore());
        assignment.setCreatedBy(userId);

        return toAssignmentResponse(
                assignmentRepository.save(assignment)
        );
    }

    @Override
    public AssignmentResponse updateAssignment(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentUpdateRequest request
    ) {
        courseAccessPolicy.requireOwner(
                courseId,
                userId
        );

        Assignment assignment =
                requireAssignment(
                        courseId,
                        assignmentId
                );

        if (request.title() != null) {
            assignment.setTitle(
                    request.title().trim()
            );
        }

        if (request.description() != null) {
            assignment.setDescription(
                    normalize(request.description())
            );
        }

        if (request.dueAt() != null) {
            assignment.setDueAt(
                    request.dueAt()
            );
        }

        if (request.maxScore() != null) {
            assignment.setMaxScore(
                    request.maxScore()
            );
        }

        return toAssignmentResponse(
                assignmentRepository.save(assignment)
        );
    }

    @Override
    public void deleteAssignment(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    ) {
        courseAccessPolicy.requireOwner(
                courseId,
                userId
        );

        Assignment assignment =
                requireAssignment(
                        courseId,
                        assignmentId
                );

        assignment.setDeletedAt(
                Instant.now()
        );
    }

    @Override
    public AssignmentSubmissionResponse submit(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentSubmissionRequest request
    ) {
        requireStudent(courseId, userId);

        Assignment assignment =
                requireAssignment(
                        courseId,
                        assignmentId
                );

        String content =
                normalize(request.content());

        boolean hasFile =
                request.file() != null &&
                        !request.file().isEmpty();

        if (content == null && !hasFile) {
            throw new ApplicationException(
                    CommonErrorCode.VALIDATION_FAILED,
                    "Vui lòng nhập nội dung hoặc đính kèm tệp bài làm."
            );
        }

        AssignmentSubmission submission =
                submissionRepository
                        .findByAssignmentIdAndStudentId(
                                assignmentId,
                                userId
                        )
                        .orElseGet(() -> {
                            AssignmentSubmission created =
                                    new AssignmentSubmission();

                            created.setAssignment(
                                    assignment
                            );
                            created.setStudentId(
                                    userId
                            );

                            return created;
                        });

        if (submission.getStatus()
                == AssignmentSubmissionStatus.GRADED) {

            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Bài làm đã được chấm và không thể nộp lại."
            );
        }

        submission.setContent(content);

        if (hasFile) {
            FileUploadResponse uploaded =
                    fileStorageService.upload(
                            request.file(),
                            "assignments/"
                                    + assignmentId
                                    + "/"
                                    + userId
                    );

            if (submission.getFileObjectName()
                    != null) {
                fileStorageService.delete(
                        submission.getFileObjectName()
                );
            }

            submission.setFileObjectName(
                    uploaded.objectName()
            );
            submission.setOriginalFileName(
                    uploaded.originalFileName()
            );
            submission.setFileContentType(
                    uploaded.contentType()
            );
            submission.setFileSize(
                    uploaded.size()
            );
        }

        submission.setStatus(
                AssignmentSubmissionStatus.SUBMITTED
        );
        submission.setSubmittedAt(
                Instant.now()
        );

        return toSubmissionResponse(
                submissionRepository.save(
                        submission
                )
        );
    }

    @Override
    public List<AssignmentSubmissionResponse>
    getMySubmissions(
            UUID courseId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        return submissionRepository
                .findAllByStudentIdAndAssignmentCourseIdAndAssignmentDeletedAtIsNull(
                        userId,
                        courseId
                )
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Override
    public List<AssignmentSubmissionResponse>
    getSubmissions(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    ) {
        courseAccessPolicy.requireOwner(
                courseId,
                userId
        );

        requireAssignment(
                courseId,
                assignmentId
        );

        return submissionRepository
                .findAllByAssignmentIdOrderBySubmittedAtDesc(
                        assignmentId
                )
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
    }

    @Override
    public AssignmentSubmissionResponse grade(
            UUID courseId,
            UUID assignmentId,
            UUID submissionId,
            UUID userId,
            AssignmentGradeRequest request
    ) {
        courseAccessPolicy.requireOwner(
                courseId,
                userId
        );

        Assignment assignment =
                requireAssignment(
                        courseId,
                        assignmentId
                );

        AssignmentSubmission submission =
                submissionRepository
                        .findByIdAndAssignmentId(
                                submissionId,
                                assignmentId
                        )
                        .orElseThrow(() ->
                                new ApplicationException(
                                        CommonErrorCode.RESOURCE_NOT_FOUND
                                )
                        );

        if (request.score()
                .compareTo(BigDecimal.ZERO) < 0
                ||
                request.score()
                        .compareTo(
                                assignment.getMaxScore()
                        ) > 0) {

            throw new ApplicationException(
                    CommonErrorCode.VALIDATION_FAILED,
                    "Điểm phải nằm trong khoảng từ 0 đến "
                            + assignment.getMaxScore()
            );
        }

        submission.setScore(
                request.score()
        );
        submission.setFeedback(
                normalize(request.feedback())
        );
        submission.setStatus(
                AssignmentSubmissionStatus.GRADED
        );
        submission.setGradedAt(
                Instant.now()
        );
        submission.setGradedBy(
                userId
        );

        return toSubmissionResponse(
                submissionRepository.save(
                        submission
                )
        );
    }

    @Override
    public void deleteMySubmission(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);

        AssignmentSubmission submission = submissionRepository
                .findByAssignmentIdAndStudentId(assignment.getId(), userId)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND
                ));

        if (submission.getStatus() == AssignmentSubmissionStatus.GRADED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Bài làm đã được chấm và không thể xóa."
            );
        }

        if (submission.getFileObjectName() != null) {
            fileStorageService.delete(submission.getFileObjectName());
        }

        submissionRepository.delete(submission);
    }

    private Assignment requireAssignment(
            UUID courseId,
            UUID assignmentId
    ) {
        return assignmentRepository
                .findByIdAndCourseIdAndDeletedAtIsNull(
                        assignmentId,
                        courseId
                )
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND
                        )
                );
    }

    private void requireStudent(
            UUID courseId,
            UUID userId
    ) {
        CourseMember member =
                courseAccessPolicy
                        .requireActiveMember(
                                courseId,
                                userId
                        );

        if (member == null ||
                member.getRole()
                        != CourseMemberRole.STUDENT) {

            throw new ApplicationException(
                    CommonErrorCode.FORBIDDEN
            );
        }
    }

    private AssignmentResponse toAssignmentResponse(
            Assignment assignment
    ) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getMaxScore(),
                assignment.getCreatedBy(),
                assignment.getDueAt()
                        .isBefore(Instant.now()),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt()
        );
    }

    private AssignmentSubmissionResponse
    toSubmissionResponse(
            AssignmentSubmission submission
    ) {
        String attachmentUrl = null;

        if (submission.getFileObjectName()
                != null) {

            attachmentUrl =
                    fileStorageService
                            .createDownloadUrl(
                                    submission
                                            .getFileObjectName()
                            )
                            .url();
        }

        return new AssignmentSubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getStudentId(),
                submission.getContent(),
                submission.getStatus(),
                submission.getSubmittedAt(),
                submission.getSubmittedAt()
                        .isAfter(
                                submission
                                        .getAssignment()
                                        .getDueAt()
                        ),
                submission.getOriginalFileName(),
                attachmentUrl,
                submission.getScore(),
                submission.getFeedback(),
                submission.getGradedAt(),
                submission.getGradedBy()
        );
    }

    private String normalize(String value) {
        if (value == null) return null;

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}