package com.smartlearning.system.user.service;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.user.dto.request.UserFilterRequest;
import com.smartlearning.system.user.entity.User;

public interface UserService {
    PagingResponse<User> handleGetUsers(UserFilterRequest filter);

    User handleAddUser(User user);
    User handleUpdateUser(User user);
    void handleDeleteUser(Long id);

}
