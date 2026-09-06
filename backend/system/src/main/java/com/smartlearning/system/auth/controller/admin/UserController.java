package com.smartlearning.system.auth.controller.admin;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserFilterRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.request.admin.AdminUserCreateRequest;
import com.smartlearning.system.auth.dto.request.admin.AdminUserUpdateRequest;
import com.smartlearning.system.auth.dto.response.UserResponse;
import com.smartlearning.system.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagingResponse<UserResponse>>> getUsers(@ModelAttribute UserFilterRequest filter) {
        PagingResponse<UserResponse> users = this.userService.handleGetUsers(filter);
        return ApiResponses.ok(users);
    }

    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<UserResponse>> addUser(
            @Valid @ModelAttribute AdminUserCreateRequest request) {
        UserResponse createdUser = this.userService.handleAddUser(request);
        return ApiResponses.created(createdUser);
    }

    @PatchMapping(
            value = "/{id}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable UUID id,
            @Valid @ModelAttribute AdminUserUpdateRequest request
    ) {
        return ApiResponses.ok(userService.handleUpdateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id
    ) {
        userService.handleDeleteUser(id);

        return ApiResponses.noContent();
    }
}
