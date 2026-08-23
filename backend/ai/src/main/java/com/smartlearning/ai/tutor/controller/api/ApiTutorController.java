package com.smartlearning.ai.tutor.controller.api;

import com.smartlearning.ai.tutor.dto.request.TutorAskRequest;
import com.smartlearning.ai.tutor.dto.response.TutorAnswerResponse;
import com.smartlearning.ai.tutor.service.TutorService;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.entity.Authorities;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses/{courseId}/ai")
@RequiredArgsConstructor
public class ApiTutorController {

    private final TutorService tutorService;

    @PostMapping("/ask")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<ApiResponse<TutorAnswerResponse>> ask(
            @PathVariable UUID courseId,
            @Valid @RequestBody TutorAskRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {

        return ApiResponses.ok(tutorService.ask(courseId, jwt.getTokenValue(), request.question()));
    }
}
