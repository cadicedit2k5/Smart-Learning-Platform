package com.smartlearning.system.user.service.impl;

import com.smartlearning.system.user.entity.Role;
import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.repository.UserRepository;
import com.smartlearning.system.user.service.RoleService;
import com.smartlearning.system.user.service.UserService;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, RoleService roleService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.roleService = roleService;
    }


    public List<User> handleGetUsers() {
        return this.userRepository.findAll();
    }

    @Override
    public User handleAddUser(User user) {
        // Hash password before save in schema
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set user default role
        Role studentRole = this.roleService.handleGetRoleByCode("STUDENT");
        user.setRole(studentRole);
        return this.userRepository.save(user);
    }

    @Override
    public User handleUpdateUser(Long id, User updateUserInfo) {
        Optional<User> u = this.userRepository.findById(id);
        if (u.isPresent()) {
            User updatedUser = u.get();
            updatedUser.setEmail(updateUserInfo.getEmail());
            updatedUser.setFullName(updateUserInfo.getFullName());
            updatedUser.setPassword(updateUserInfo.getPassword());
            return this.userRepository.save(updatedUser);
        }
        return null;
    }

    @Override
    public void handleDeleteUser(Long id) {
        System.out.println("chua lam");
    }

}
