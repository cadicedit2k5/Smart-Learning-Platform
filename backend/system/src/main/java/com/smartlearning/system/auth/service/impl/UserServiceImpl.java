package com.smartlearning.system.auth.service.impl;

import com.smartlearning.common.dto.response.pagination.PagingResponse;
import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.storage.dto.FileUploadResponse;
import com.smartlearning.storage.service.FileStorageService;
import com.smartlearning.system.auth.dto.request.UserCreateRequest;
import com.smartlearning.system.auth.dto.request.UserFilterRequest;
import com.smartlearning.system.auth.dto.request.UserLoginRequest;
import com.smartlearning.system.auth.dto.request.UserUpdateRequest;
import com.smartlearning.system.auth.dto.response.LoginResponse;
import com.smartlearning.system.auth.dto.response.UserResponse;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.mapper.UserMapper;
import com.smartlearning.system.auth.repository.UserRepository;
import com.smartlearning.system.auth.security.SecurityUser;
import com.smartlearning.system.auth.service.JwtTokenService;
import com.smartlearning.system.auth.service.RoleService;
import com.smartlearning.system.auth.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleService roleService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenService jwtTokenService;
    private final FileStorageService fileStorageService;

    public PagingResponse<UserResponse> handleGetUsers(UserFilterRequest filter) {
        Page<UserResponse> pages = userRepository.findAll(filter.specification(), filter.pageable())
                .map(this.userMapper::toResponse);
        return PagingResponse.from(pages);
    }

    @Override
    public UserResponse handleAddUser(UserCreateRequest request) {
        String normalizedEmail = request
                .getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(
                normalizedEmail
        )) {
            throw new ApplicationException(
                    CommonErrorCode.DATA_CONFLICT,
                    "Email đã được sử dụng"
            );
        }

        User user = this.userMapper.toEntity(request);
        user.setEmail(normalizedEmail);
        // Hash password before save in schema
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        // Set user default role
        Role studentRole = this.roleService.handleGetRoleByCode("STUDENT");
        user.setRole(studentRole);

        User saveUser = this.userRepository.save(user);

        // Process upload file to MinIO
        if (request.getAvatar() != null && !request.getAvatar().isEmpty()) {
            String folder = "system/users/" + user.getId() + "/avatar";
            FileUploadResponse uploadedFile = fileStorageService.upload(request.getAvatar(), folder);
            user.setAvatar(uploadedFile.objectName());
        }

        return this.userMapper.toResponse(saveUser);
    }

    @Override
    public UserResponse handleUpdateUser(UUID id, UserUpdateRequest request) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED).orElseThrow(
                        () -> new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND,
                                "User không tồn tại!")
                        );

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
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() -> new ApplicationException(
                        CommonErrorCode.RESOURCE_NOT_FOUND,
                        "User không tồn tại!")
                );

        user.setStatus(UserStatus.DELETED);
    }

    @Override
    public UserResponse handleGetCurrentUser(UUID id) {
        User user = userRepository.findByIdAndStatusNot(id, UserStatus.DELETED)
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND,
                                "Không tìm thấy người dùng"
                        )
                );

        return userMapper.toResponse(user);
    }

    @Override
    public LoginResponse handleLogin(UserLoginRequest request) {
        String normalizedEmail = request
                .email()
                .trim()
                .toLowerCase(Locale.ROOT);

        try {
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    normalizedEmail,
                                    request.password()
                            )
                    );

            SecurityUser securityUser =
                    (SecurityUser) authentication.getPrincipal();

            return jwtTokenService.generateToken(
                    securityUser
            );
        } catch (AuthenticationException exception) {
            throw new ApplicationException(
                    CommonErrorCode.UNAUTHORIZED,
                    "Thông tin đăng nhập không hợp lệ"
            );
        }
    }
}
