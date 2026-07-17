package com.smartlearning.system.user.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.user.dto.request.UserCreateRequest;
import com.smartlearning.system.user.dto.request.UserFilterRequest;
import com.smartlearning.system.user.dto.response.UserResponse;
import com.smartlearning.system.user.entity.User;

public interface UserService {
    PagingResponse<UserResponse> handleGetUsers(UserFilterRequest filter);

    UserResponse handleAddUser(UserCreateRequest request);
    UserResponse handleUpdateUser(User user);
    void handleDeleteUser(Long id);

}
