package com.smartlearning.system.user.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.user.dto.request.UserFilterRequest;
import com.smartlearning.system.user.dto.response.UserResponse;
import com.smartlearning.system.user.entity.Role;
import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.repository.UserRepository;
import com.smartlearning.system.user.service.RoleService;
import com.smartlearning.system.user.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;

    public PagingResponse<UserResponse> handleGetUsers(UserFilterRequest filter) {
        Page<UserResponse> pages = userRepository.findAll(filter.specification(), filter.pageable())
                .map(UserResponse::from);
        return PagingResponse.from(pages);
    }

    @Override
    public UserResponse handleAddUser(User user) {
        // Hash password before save in schema
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // Set user default role
        Role studentRole = this.roleService.handleGetRoleByCode("STUDENT");
        user.setRole(studentRole);
        return UserResponse.from(this.userRepository.save(user));
    }

    @Override
    public UserResponse handleUpdateUser(User user) {
        final Long id = user.getId();
        if (userRepository.existsById(id)) {
            return UserResponse.from(userRepository.save(user));
        }
        throw new RuntimeException("User không tồn tại!!");
    }

    @Override
    public void handleDeleteUser(Long id) {
        System.out.println("chua lam");
    }

}
