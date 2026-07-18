package com.smartlearning.system.user.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.system.user.dto.request.UserCreateRequest;
import com.smartlearning.system.user.dto.request.UserFilterRequest;
import com.smartlearning.system.user.dto.request.UserUpdateRequest;
import com.smartlearning.system.user.dto.response.UserResponse;
import com.smartlearning.system.user.entity.Role;
import com.smartlearning.system.user.entity.User;
import com.smartlearning.system.user.mapper.UserMapper;
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
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserMapper userMapper;

    public PagingResponse<UserResponse> handleGetUsers(UserFilterRequest filter) {
        Page<UserResponse> pages = userRepository.findAll(filter.specification(), filter.pageable())
                .map(this.userMapper::toResponse);
        return PagingResponse.from(pages);
    }

    @Override
    public UserResponse handleAddUser(UserCreateRequest request) {
        User user = this.userMapper.toEntity(request);
        // Hash password before save in schema
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set user default role
        Role studentRole = this.roleService.handleGetRoleByCode("STUDENT");
        user.setRole(studentRole);
//        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
//            user.setAvatar();
//        }
        User saveUser = this.userRepository.save(user);
        return this.userMapper.toResponse(saveUser);
    }

    @Override
    public UserResponse handleUpdateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findById(id).orElseThrow(
                        () -> new RuntimeException("User không tồn tại"));

        userMapper.partialUpdate(request, user);

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(
                    passwordEncoder.encode(
                            request.getPassword()
                    )
            );
        }

        User savedUser = userRepository.save(user);
        return userMapper.toResponse(savedUser);
    }

    @Override
    public void handleDeleteUser(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User không tồn tại"));

        userRepository.delete(user);
    }

}
