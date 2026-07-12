package com.smartlearning.system.user.service;

import com.smartlearning.system.user.entity.User;

import java.util.List;

public interface UserService {
    List<User> handleGetUsers();

    User handleAddUser(User user);
    User handleUpdateUser(Long id, User updateUserInfo);
    void handleDeleteUser(Long id);

}
