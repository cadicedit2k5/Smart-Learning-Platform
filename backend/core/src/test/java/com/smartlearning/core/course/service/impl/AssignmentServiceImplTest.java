package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.response.AssignmentResponse;
import com.smartlearning.core.course.entity.Assignment;
import com.smartlearning.core.course.entity.Course;
import com.smartlearning.core.course.entity.enums.AssignmentStatus;
import com.smartlearning.core.course.entity.enums.CourseStatus;
import com.smartlearning.core.course.repository.AssignmentRepository;
import com.smartlearning.core.course.repository.AssignmentSubmissionRepository;
import com.smartlearning.core.course.repository.CourseRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.notification.entity.enums.NotificationType;
import com.smartlearning.core.notification.service.NotificationService;
import com.smartlearning.storage.service.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.course;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssignmentServiceImplTest {

    private static final UUID ASSIGNMENT_ID =
            UUID.fromString("90000000-0000-0000-0000-000000000001");

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private AssignmentSubmissionRepository submissionRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseAccessPolicy courseAccessPolicy;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssignmentServiceImpl assignmentService;

    @Test
    void publishAssignment_publishesAssignmentWhenCourseIsPublished() {
        Course course = course();
        course.setStatus(CourseStatus.PUBLISHED);

        Assignment assignment = assignment(course, AssignmentStatus.DRAFT, Instant.now().plusSeconds(3600));

        when(assignmentRepository.findByIdAndCourseIdAndDeletedAtIsNull(ASSIGNMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(assignment));

        AssignmentResponse result =
                assignmentService.publishAssignment(COURSE_ID, ASSIGNMENT_ID, OWNER_ID);

        assertThat(result.status()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.PUBLISHED);
        assertThat(assignment.getPublishedAt()).isNotNull();

        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
        verify(notificationService).createForCourseStudents(
                COURSE_ID,
                NotificationType.ASSIGNMENT_CREATED,
                "Bài tập mới",
                "Giảng viên đã giao bài tập: Bài tập 1"
        );
    }

    @Test
    void publishAssignment_rejectsDraftCourse() {
        Course course = course();
        course.setStatus(CourseStatus.DRAFT);

        Assignment assignment = assignment(course, AssignmentStatus.DRAFT, Instant.now().plusSeconds(3600));

        when(assignmentRepository.findByIdAndCourseIdAndDeletedAtIsNull(ASSIGNMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() ->
                assignmentService.publishAssignment(COURSE_ID, ASSIGNMENT_ID, OWNER_ID)
        )
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(assignment.getPublishedAt()).isNull();

        verifyNoInteractions(notificationService);
    }

    @Test
    void publishAssignment_rejectsArchivedCourse() {
        Course course = course();
        course.setStatus(CourseStatus.ARCHIVED);

        Assignment assignment = assignment(course, AssignmentStatus.DRAFT, Instant.now().plusSeconds(3600));

        when(assignmentRepository.findByIdAndCourseIdAndDeletedAtIsNull(ASSIGNMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() ->
                assignmentService.publishAssignment(COURSE_ID, ASSIGNMENT_ID, OWNER_ID)
        )
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(assignment.getPublishedAt()).isNull();

        verifyNoInteractions(notificationService);
    }

    @Test
    void publishAssignment_rejectsAssignmentThatIsNotDraft() {
        Course course = course();
        course.setStatus(CourseStatus.PUBLISHED);

        Assignment assignment = assignment(course, AssignmentStatus.PUBLISHED, Instant.now().plusSeconds(3600));

        when(assignmentRepository.findByIdAndCourseIdAndDeletedAtIsNull(ASSIGNMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() ->
                assignmentService.publishAssignment(COURSE_ID, ASSIGNMENT_ID, OWNER_ID)
        )
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        verifyNoInteractions(notificationService);
    }

    @Test
    void publishAssignment_rejectsExpiredDeadline() {
        Course course = course();
        course.setStatus(CourseStatus.PUBLISHED);

        Assignment assignment = assignment(course, AssignmentStatus.DRAFT, Instant.now().minusSeconds(60));

        when(assignmentRepository.findByIdAndCourseIdAndDeletedAtIsNull(ASSIGNMENT_ID, COURSE_ID))
                .thenReturn(Optional.of(assignment));

        assertThatThrownBy(() ->
                assignmentService.publishAssignment(COURSE_ID, ASSIGNMENT_ID, OWNER_ID)
        )
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.VALIDATION_FAILED);

        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.DRAFT);
        assertThat(assignment.getPublishedAt()).isNull();

        verifyNoInteractions(notificationService);
    }

    private Assignment assignment(Course course, AssignmentStatus status, Instant dueAt) {
        Assignment assignment = new Assignment();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setCourse(course);
        assignment.setTitle("Bài tập 1");
        assignment.setDescription("Mô tả bài tập");
        assignment.setDueAt(dueAt);
        assignment.setMaxScore(BigDecimal.TEN);
        assignment.setCreatedBy(OWNER_ID);
        assignment.setStatus(status);
        return assignment;
    }
}