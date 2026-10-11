
package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.*;
import com.smartlearning.core.course.service.TopicQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/courses/{courseId}/topics/{topicId}/quizzes")
public class ApiTopicQuizController {

    private final TopicQuizService quizService;

    @GetMapping("/manage")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<List<TopicQuizEditorResponse>>> getVersions(
            @PathVariable UUID courseId, @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.getVersions(courseId, topicId, JwtUtils.getUserId(jwt))
        );
    }

    @PostMapping("/draft")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<TopicQuizEditorResponse>> createDraft(
            @PathVariable UUID courseId, @PathVariable UUID topicId,
            @Valid @RequestBody TopicQuizCreateRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                quizService.createDraft(courseId, topicId, JwtUtils.getUserId(jwt), request)
        );
    }

    @PutMapping("/{quizId}/questions")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<TopicQuizEditorResponse>> updateQuestions(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID quizId,
            @Valid @RequestBody TopicQuizQuestionsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.updateQuestions(courseId, topicId, quizId, JwtUtils.getUserId(jwt), request)
        );
    }

    @PatchMapping("/{quizId}/details")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<TopicQuizEditorResponse>> updateDetails(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID quizId,
            @Valid @RequestBody TopicQuizDetailsRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.updateDetails(courseId, topicId, quizId, JwtUtils.getUserId(jwt), request)
        );
    }

    @PostMapping("/{quizId}/next-version")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<TopicQuizEditorResponse>> createNextVersion(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID quizId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                quizService.createNextVersion(courseId, topicId, quizId, JwtUtils.getUserId(jwt))
        );
    }

    @PostMapping("/{quizId}/publish")
    @PreAuthorize(Authorities.COURSE_MANAGE)
    public ResponseEntity<ApiResponse<TopicQuizEditorResponse>> publish(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID quizId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.publish(courseId, topicId, quizId, JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/current")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TopicQuizStudentResponse>> getCurrent(
            @PathVariable UUID courseId, @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.getCurrent(courseId, topicId, JwtUtils.getUserId(jwt))
        );
    }

    @PostMapping("/{quizId}/attempts")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TopicQuizAttemptResponse>> submit(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID quizId,
            @Valid @RequestBody TopicQuizSubmitRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.created(
                quizService.submit(courseId, topicId, quizId, JwtUtils.getUserId(jwt), request)
        );
    }

    @GetMapping("/attempts")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<List<TopicQuizAttemptSummaryResponse>>> getHistory(
            @PathVariable UUID courseId, @PathVariable UUID topicId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.getHistory(courseId, topicId, JwtUtils.getUserId(jwt))
        );
    }

    @GetMapping("/attempts/{attemptId}")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TopicQuizAttemptResponse>> getAttempt(
            @PathVariable UUID courseId, @PathVariable UUID topicId, @PathVariable UUID attemptId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                quizService.getAttempt(courseId, topicId, attemptId, JwtUtils.getUserId(jwt))
        );
    }
}
