package com.smartlearning.system.user.controller;

import com.smartlearning.common.dto.response.ApiResponse;
import com.smartlearning.common.dto.response.ApiResponses;
import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.user.dto.request.UserFilterRequest;
import com.smartlearning.system.user.dto.response.UserResponse;
import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
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
            @ModelAttribute User user,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) {
        if (avatar != null && !avatar.isEmpty()) {
            user.setAvatar(avatar.getOriginalFilename());
        }

        UserResponse createdUser = this.userService.handleAddUser(user);
        return ApiResponses.created(createdUser);
    }

}
