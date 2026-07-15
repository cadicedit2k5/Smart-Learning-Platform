package com.smartlearning.system.user.service;

import com.smartlearning.common.dto.response.pagging.PageResponse;
import com.smartlearning.system.user.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {
    PageResponse<User> handleGetUsers();

    User handleAddUser(User user);
    User handleUpdateUser(User user);
    void handleDeleteUser(Long id);

}
