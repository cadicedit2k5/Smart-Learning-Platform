package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.AssignmentResponse;
import com.smartlearning.core.course.dto.response.AssignmentSubmissionResponse;
import com.smartlearning.core.course.entity.Assignment;
import com.smartlearning.core.course.entity.AssignmentSubmission;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.enums.AssignmentStatus;
import com.smartlearning.core.course.entity.enums.AssignmentSubmissionStatus;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.repository.AssignmentRepository;
import com.smartlearning.core.course.repository.AssignmentSubmissionRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.AssignmentService;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import com.smartlearning.core.notification.service.NotificationService;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class AssignmentServiceImpl implements AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository submissionRepository;
    private final CourseRepository courseRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final FileStorageService fileStorageService;
    private final NotificationService notificationService;

    @Scheduled(fixedDelay = 60_000)
    public void closeOverdueAssignments() {
        assignmentRepository.closeOverdueAssignments(
                AssignmentStatus.PUBLISHED,
                AssignmentStatus.CLOSED,
                Instant.now()
        );
    }

    @Override
    public List<AssignmentResponse> getAssignments(UUID courseId, UUID userId) {
        CourseMember member = courseAccessPolicy.requireActiveMember(courseId, userId);
        boolean canManage = member == null || member.getRole() == CourseMemberRole.OWNER;

        return assignmentRepository.findAllByCourseIdOrdered(courseId).stream()
                .filter(assignment -> canManage || assignment.getStatus() != AssignmentStatus.DRAFT)
                .map(this::toAssignmentResponse)
                .toList();
    }

    @Override
    public AssignmentResponse createAssignment(UUID courseId, UUID userId, AssignmentCreateRequest request) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        Assignment assignment = new Assignment();
        assignment.setCourse(course);
        assignment.setTitle(request.title().trim());
        assignment.setDescription(normalize(request.description()));
        assignment.setDueAt(request.dueAt());
        assignment.setMaxScore(request.maxScore());
        assignment.setCreatedBy(userId);
        assignment.setStatus(AssignmentStatus.DRAFT);

        return toAssignmentResponse(assignmentRepository.save(assignment));
    }

    @Override
    public AssignmentResponse updateAssignment(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentUpdateRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);

        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Bài tập đã có bài nộp và không thể chỉnh sửa nội dung."
            );
        }

        boolean contentChanged = false;

        if (request.title() != null) {
            String title = request.title().trim();

            if (!title.equals(assignment.getTitle())) {
                assignment.setTitle(title);
                contentChanged = true;
            }
        }

        if (request.description() != null) {
            String description = normalize(request.description());

            if (!Objects.equals(description, assignment.getDescription())) {
                assignment.setDescription(description);
                contentChanged = true;
            }
        }

        if (request.maxScore() != null && request.maxScore().compareTo(assignment.getMaxScore()) != 0) {
            assignment.setMaxScore(request.maxScore());
            contentChanged = true;
        }

        if (request.dueAt() != null && !request.dueAt().equals(assignment.getDueAt())) {
            if (assignment.getStatus() != AssignmentStatus.DRAFT) {
                throw new ApplicationException(
                        CommonErrorCode.DATA_CONFLICT,
                        "Hãy sử dụng chức năng gia hạn để thay đổi deadline của bài tập đã đăng."
                );
            }

            assignment.setDueAt(request.dueAt());
        }

        if (contentChanged && assignment.getStatus() != AssignmentStatus.DRAFT) {
            notificationService.createForCourseStudents(
                    courseId,
                    NotificationType.ASSIGNMENT_UPDATED,
                    "Bài tập đã được cập nhật",
                    "Giảng viên đã cập nhật bài tập: " + assignment.getTitle()
            );
        }

        return toAssignmentResponse(assignment);
    }

    @Override
    public AssignmentResponse publishAssignment(UUID courseId, UUID assignmentId, UUID userId) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);
        requireDraft(assignment);

        if (assignment.getCourse().getStatus() != CourseStatus.PUBLISHED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Chỉ có thể đăng bài tập khi khóa học đã được xuất bản."
            );
        }

        Instant now = Instant.now();

        if (!assignment.getDueAt().isAfter(now)) {
            throw new ApplicationException(CommonErrorCode.VALIDATION_FAILED, "Hạn nộp phải nằm sau thời điểm đăng bài.");
        }

        assignment.setStatus(AssignmentStatus.PUBLISHED);
        assignment.setPublishedAt(now);

        notificationService.createForCourseStudents(
                courseId,
                NotificationType.ASSIGNMENT_CREATED,
                "Bài tập mới",
                "Giảng viên đã giao bài tập: " + assignment.getTitle()
        );

        return toAssignmentResponse(assignment);
    }

    @Override
    public AssignmentResponse closeAssignment(UUID courseId, UUID assignmentId, UUID userId) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);
        Instant now = Instant.now();

        closeIfOverdue(assignment, now);

        if (assignment.getStatus() != AssignmentStatus.PUBLISHED) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Chỉ có thể đóng bài tập đang mở."
            );
        }

        assignment.setStatus(AssignmentStatus.CLOSED);
        assignment.setClosedAt(now);

        return toAssignmentResponse(assignment);
    }

    @Override
    public void deleteAssignment(UUID courseId, UUID assignmentId, UUID userId) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);

        if (submissionRepository.existsByAssignmentId(assignmentId)) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Bài tập đã có bài nộp và không thể xóa."
            );
        }

        assignment.setDeletedAt(Instant.now());
    }

    @Override
    public AssignmentSubmissionResponse submit(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentSubmissionRequest request
    ) {
        requireStudent(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);
        requireOpenForSubmission(assignment);

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

        boolean alreadyGraded =
                submission.getStatus() == AssignmentSubmissionStatus.GRADED;

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

        AssignmentSubmission saved = submissionRepository.save(submission);

        notificationService.create(
                saved.getStudentId(),
                NotificationType.ASSIGNMENT_GRADED,
                alreadyGraded ? "Điểm bài tập đã được cập nhật" : "Bài tập đã được chấm",
                assignment.getTitle() + ": " + saved.getScore() + "/" + assignment.getMaxScore(),
                courseId
        );

        return toSubmissionResponse(saved);
    }

    @Override
    public AssignmentResponse extendDeadline(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentDeadlineUpdateRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);
        Instant now = Instant.now();

        closeIfOverdue(assignment, now);

        if (assignment.getStatus() == AssignmentStatus.DRAFT) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Bài tập nháp có thể chỉnh sửa deadline trực tiếp."
            );
        }

        if (!request.dueAt().isAfter(now)) {
            throw new ApplicationException(
                    CommonErrorCode.VALIDATION_FAILED,
                    "Deadline mới phải nằm trong tương lai."
            );
        }

        if (!request.dueAt().isAfter(assignment.getDueAt())) {
            throw new ApplicationException(
                    CommonErrorCode.VALIDATION_FAILED,
                    "Deadline mới phải muộn hơn deadline hiện tại."
            );
        }

        assignment.setDueAt(request.dueAt());

        if (assignment.getStatus() == AssignmentStatus.CLOSED) {
            assignment.setStatus(AssignmentStatus.PUBLISHED);
            assignment.setClosedAt(null);
        }

        notificationService.createForCourseStudents(
                courseId,
                NotificationType.ASSIGNMENT_UPDATED,
                "Hạn nộp bài tập đã thay đổi",
                "Giảng viên đã gia hạn bài tập: " + assignment.getTitle()
        );

        return toAssignmentResponse(assignment);
    }

    @Override
    public void deleteMySubmission(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    ) {
        requireStudent(courseId, userId);

        Assignment assignment = requireAssignment(courseId, assignmentId);
        requireOpenForSubmission(assignment);

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

    private void requireDraft(Assignment assignment) {
        if (assignment.getStatus() != AssignmentStatus.DRAFT) {
            throw new ApplicationException(CommonErrorCode.DATA_CONFLICT, "Chỉ có thể chỉnh sửa bài tập đang ở trạng thái nháp.");
        }
    }

    private AssignmentStatus effectiveStatus(Assignment assignment) {
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED && !assignment.getDueAt().isAfter(Instant.now())) {
            return AssignmentStatus.CLOSED;
        }

        return assignment.getStatus();
    }

    private void requireOpenForSubmission(Assignment assignment) {
        if (effectiveStatus(assignment) != AssignmentStatus.PUBLISHED) {
            throw new ApplicationException(CommonErrorCode.DATA_CONFLICT, "Bài tập đã đóng!");
        }
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

    private void closeIfOverdue(Assignment assignment, Instant now) {
        if (assignment.getStatus() == AssignmentStatus.PUBLISHED && !assignment.getDueAt().isAfter(now)) {
            assignment.setStatus(AssignmentStatus.CLOSED);
            assignment.setClosedAt(assignment.getDueAt());
        }
    }

    private AssignmentResponse toAssignmentResponse(Assignment assignment) {
        return new AssignmentResponse(
                assignment.getId(),
                assignment.getCourse().getId(),
                assignment.getTitle(),
                assignment.getDescription(),
                assignment.getDueAt(),
                assignment.getMaxScore(),
                assignment.getCreatedBy(),
                effectiveStatus(assignment),
                assignment.getPublishedAt(),
                assignment.getClosedAt(),
                !assignment.getDueAt().isAfter(Instant.now()),
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