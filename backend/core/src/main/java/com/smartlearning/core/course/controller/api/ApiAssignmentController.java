package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.*;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.AssignmentCreateRequest;
import com.smartlearning.core.course.dto.request.AssignmentGradeRequest;
import com.smartlearning.core.course.dto.request.AssignmentSubmissionRequest;
import com.smartlearning.core.course.dto.request.AssignmentUpdateRequest;
import com.smartlearning.core.course.dto.response.AssignmentResponse;
import com.smartlearning.core.course.dto.response.AssignmentSubmissionResponse;
import com.smartlearning.core.course.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/assignments")
@RequiredArgsConstructor
public class ApiAssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<
            ApiResponse<List<AssignmentResponse>>
            > getAssignments(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.getAssignments(
                        courseId,
                        JwtUtils.getUserId(jwt)
                )
        );
    }

    @PostMapping
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<AssignmentResponse>>
    create(
            @PathVariable UUID courseId,
            @Valid
            @RequestBody AssignmentCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                assignmentService.createAssignment(
                        courseId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }

    @PatchMapping("/{assignmentId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<AssignmentResponse>>
    update(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @Valid
            @RequestBody AssignmentUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.updateAssignment(
                        courseId,
                        assignmentId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }

    @DeleteMapping("/{assignmentId}")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<Void> delete(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        assignmentService.deleteAssignment(
                courseId,
                assignmentId,
                JwtUtils.getUserId(jwt)
        );

        return ApiResponses.noContent();
    }

    @PostMapping(
            value = "/{assignmentId}/submission",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<
            ApiResponse<AssignmentSubmissionResponse>
            > submit(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @Valid
            @ModelAttribute AssignmentSubmissionRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.submit(
                        courseId,
                        assignmentId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }

    @GetMapping("/my-submissions")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<
            ApiResponse<List<AssignmentSubmissionResponse>>
            > getMySubmissions(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.getMySubmissions(
                        courseId,
                        JwtUtils.getUserId(jwt)
                )
        );
    }

    @GetMapping("/{assignmentId}/submissions")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<
            ApiResponse<List<AssignmentSubmissionResponse>>
            > getSubmissions(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.getSubmissions(
                        courseId,
                        assignmentId,
                        JwtUtils.getUserId(jwt)
                )
        );
    }

    @PatchMapping(
            "/{assignmentId}/submissions/{submissionId}/grade"
    )
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<
            ApiResponse<AssignmentSubmissionResponse>
            > grade(
            @PathVariable UUID courseId,
            @PathVariable UUID assignmentId,
            @PathVariable UUID submissionId,
            @Valid
            @RequestBody AssignmentGradeRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                assignmentService.grade(
                        courseId,
                        assignmentId,
                        submissionId,
                        JwtUtils.getUserId(jwt),
                        request
                )
        );
    }
}
