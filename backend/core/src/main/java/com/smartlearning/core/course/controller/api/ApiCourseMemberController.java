package com.smartlearning.core.course.controller.api;

import com.smartlearning.common.entity.Authorities;
import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.utils.JwtUtils;
import com.smartlearning.core.course.dto.request.CourseMemberCreateRequest;
import com.smartlearning.core.course.dto.request.JoinCourseRequest;
import com.smartlearning.core.course.dto.response.CourseMemberResponse;
import com.smartlearning.core.course.service.CourseMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/courses")
@RequiredArgsConstructor
public class ApiCourseMemberController {

    private final CourseMemberService memberService;

    @PreAuthorize(Authorities.COURSE_MANAGE)
    @PostMapping("/{courseId}/members")
    public ResponseEntity<ApiResponse<CourseMemberResponse>> addMember(
            @PathVariable UUID courseId,
            @Valid @RequestBody
            CourseMemberCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        return ApiResponses.created(
                memberService.addMember(
                        courseId,
                        request,
                        JwtUtils.getUserId(jwt)
                )
        );
    }

//    @PreAuthorize("hasAuthority('COURSE_MANAGE')")
//    @DeleteMapping("/{courseId}/members/{memberId}")
//    public ResponseEntity<Void> removeMember(
//            @PathVariable UUID courseId,
//            @PathVariable UUID memberId,
//            @AuthenticationPrincipal Jwt jwt
//    ) {
//        memberService.removeMember(
//                courseId,
//                memberId,
//                JwtUtils.getUserId(jwt)
//        );
//
//        return ApiResponses.noContent();
//    }
//
    @PreAuthorize(Authorities.COURSE_READ)
    @PostMapping("/join")
    public ResponseEntity<ApiResponse<CourseMemberResponse>>
    joinByCode(
            @Valid @RequestBody JoinCourseRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ApiResponses.ok(
                memberService.joinByCode(
                        request,
                        JwtUtils.getUserId(jwt)
                )
        );
    }
}
