package com.smartlearning.system.user.service;

import com.smartlearning.common.dto.response.pagination.PageResponse;
import com.smartlearning.system.user.entity.User;

public interface UserService {
    PageResponse<User> handleGetUsers();

    User handleAddUser(User user);
    User handleUpdateUser(User user);
    void handleDeleteUser(Long id);

}
