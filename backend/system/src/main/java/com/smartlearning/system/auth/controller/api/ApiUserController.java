package com.smartlearning.system.auth.controller.api;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.entity.Authorities;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserFilterRequest;
import com.smartlearning.system.auth.dto.request.UserLoginRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.dto.response.UserLookupResponse;
import com.smartlearning.system.auth.dto.response.UserResponse;
import com.smartlearning.system.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ApiUserController {
    private final UserService userService;

    @PostMapping(
            value = "/auth/register",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @ModelAttribute UserCreateRequest request
    ) {
        return ApiResponses.created(
                userService.handleAddUser(request)
        );
    }

    @PostMapping("/auth/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody UserLoginRequest request
    ) {
        return ApiResponses.ok(
                userService.handleLogin(request)
        );
    }

    @GetMapping("/auth/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(
                Objects.requireNonNull(jwt.getSubject())
        );

        return ApiResponses.ok(
                userService.handleGetCurrentUser(userId)
        );
    }

    @PatchMapping(
            value = "/auth/me",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @ModelAttribute UserUpdateRequest request
    ) {
        return ApiResponses.ok(userService.handleUpdateUser(
                        UUID.fromString(Objects.requireNonNull(jwt.getSubject())),
                        request));
    }

    @PreAuthorize(Authorities.USER_READ)
    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PagingResponse<UserLookupResponse>>> searchUsers(
            @ModelAttribute UserFilterRequest filter
    ) {
        return ApiResponses.ok(userService.handleSearchUsers(filter));
    }

    @PreAuthorize(Authorities.USER_READ)
    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserLookupResponse>> getUser(
            @PathVariable UUID id
    ) {
        return ApiResponses.ok(userService.handleGetUserLookup(id));
    }
}
