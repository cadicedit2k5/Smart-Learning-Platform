package com.smartlearning.system.user.service;

import com.smartlearning.system.user.entity.User;

import java.util.List;

public interface UserService {
    List<User> handleGetUsers();

    User handleAddUser(User user);
    User handleUpdateUser(User user);
    void handleDeleteUser(Long id);

}
