package com.smartlearning.system.auth.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.system.auth.dto.request.admin.RoleCreateRequest;
import com.smartlearning.system.auth.dto.request.admin.RoleUpdateRequest;
import com.smartlearning.system.auth.dto.response.RoleDetailResponse;
import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.error.AuthErrorCode;
import com.smartlearning.system.auth.repository.PermissionRepository;
import com.smartlearning.system.auth.repository.RoleRepository;
import com.smartlearning.system.auth.repository.UserRepository;
import com.smartlearning.system.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    @Override
    public Role handleGetRoleByCode(String code) {
        return this.roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Role %s chưa được khởi tạo", code)
                ));
    }

    @Override
    public List<RoleDetailResponse> getRoles() {
        return roleRepository.findAllWithPermissions()
                .stream().sorted(Comparator.comparing(Role::getCode))
                .map(RoleDetailResponse::from)
                .toList();
    }

    @Override
    public RoleDetailResponse getRole(Long roleId) {
        return RoleDetailResponse.from(
                requireRole(roleId)
        );
    }

    @Override
    public RoleDetailResponse updateRole(Long roleId, RoleUpdateRequest request) {

        Role role = requireRole(roleId);

        if (request.name() != null) {
            role.setName(request.name().trim());
        }

        if (role.getCode().equalsIgnoreCase("ADMIN")) {
            throw new ApplicationException(AuthErrorCode.ADMIN_PERMISSIONS_PROTECTED);
        }

        role.setPermissions(new HashSet<>(resolvePermissions(request.permissionIds())));

        return RoleDetailResponse.from(role);
    }

    private List<Permission> resolvePermissions(Set<Long> permissionIds) {

        if (permissionIds == null || permissionIds.isEmpty()) {
            return List.of();
        }

        List<Permission> permissions = permissionRepository.findAllById(permissionIds);

        if (permissions.size() != permissionIds.size()) {
            throw new ApplicationException(AuthErrorCode.PERMISSION_NOT_FOUND);
        }

        return permissions;
    }

    private Role requireRole(Long roleId) {
        return roleRepository.findByIdWithPermissions(roleId).orElseThrow(() ->
                        new ApplicationException(AuthErrorCode.ROLE_NOT_FOUND));
    }
}
