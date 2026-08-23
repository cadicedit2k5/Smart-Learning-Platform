package com.smartlearning.core.course.controller.internal;

import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/internal/courses")
@RequiredArgsConstructor
public class InternalCourseAccessController {

    private final CourseAccessPolicy courseAccessPolicy;

    @GetMapping("/{courseId}/access")
    @PreAuthorize(Authorities.COURSE_READ)
    public ResponseEntity<Void> requireAccess(
            @PathVariable UUID courseId,
            @AuthenticationPrincipal Jwt jwt
    ) {

        courseAccessPolicy.requireActiveMember(
                courseId,
                JwtUtils.getUserId(jwt)
        );

        return ResponseEntity.noContent().build();
    }
}