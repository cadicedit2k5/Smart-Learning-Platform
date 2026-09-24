package com.smartlearning.core.course.service;



import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.AssignmentResponse;
import com.smartlearning.core.course.dto.response.AssignmentSubmissionResponse;

import java.util.List;
import java.util.UUID;

public interface AssignmentService {

    List<AssignmentResponse> getAssignments(UUID courseId, UUID userId);

    AssignmentResponse createAssignment(UUID courseId, UUID userId, AssignmentCreateRequest request);

    AssignmentResponse updateAssignment(UUID courseId, UUID assignmentId, UUID userId, AssignmentUpdateRequest request);

    AssignmentResponse publishAssignment(UUID courseId, UUID assignmentId, UUID userId);

    AssignmentResponse closeAssignment(UUID courseId, UUID assignmentId, UUID userId);

    void deleteAssignment(UUID courseId, UUID assignmentId, UUID userId);

    AssignmentSubmissionResponse submit(
            UUID courseId,
            UUID assignmentId,
            UUID userId,
            AssignmentSubmissionRequest request
    );

    List<AssignmentSubmissionResponse> getMySubmissions(
            UUID courseId,
            UUID userId
    );

    List<AssignmentSubmissionResponse> getSubmissions(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    );

    AssignmentSubmissionResponse grade(
            UUID courseId,
            UUID assignmentId,
            UUID submissionId,
            UUID userId,
            AssignmentGradeRequest request
    );

    void deleteMySubmission(
            UUID courseId,
            UUID assignmentId,
            UUID userId
    );

    AssignmentResponse extendDeadline(UUID courseId, UUID assignmentId, UUID userId, AssignmentDeadlineUpdateRequest request);

    AssignmentResponse reopenAssignment(UUID courseId, UUID assignmentId, UUID userId);
}
