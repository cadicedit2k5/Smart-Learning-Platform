package com.smartlearning.system.auth.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserFilterRequest;
import com.smartlearning.system.auth.dto.request.UserLoginRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.dto.response.UserResponse;

import java.util.UUID;

public interface UserService {
    PagingResponse<UserResponse> handleGetUsers(UserFilterRequest filter);

    UserResponse handleAddUser(UserCreateRequest request);
    UserResponse handleUpdateUser(UUID id, UserUpdateRequest request);
    void handleDeleteUser(UUID id);
    UserResponse handleGetCurrentUser(UUID id);

    LoginResponse handleLogin(UserLoginRequest request);


}
