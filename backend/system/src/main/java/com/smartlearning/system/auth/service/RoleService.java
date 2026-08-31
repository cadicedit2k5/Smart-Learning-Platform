package com.smartlearning.system.auth.service;

import com.smartlearning.system.auth.dto.request.admin.RoleCreateRequest;
import com.smartlearning.system.auth.dto.request.admin.RoleUpdateRequest;
import com.smartlearning.system.auth.dto.response.PermissionResponse;
import com.smartlearning.system.auth.dto.response.RoleDetailResponse;
import com.smartlearning.system.auth.entity.Role;

import java.util.List;

public interface RoleService {
    Role handleGetRoleByCode(String code);

    List<RoleDetailResponse> getRoles();

    RoleDetailResponse getRole(Long roleId);

    RoleDetailResponse createRole(RoleCreateRequest request);

    RoleDetailResponse updateRole(Long roleId, RoleUpdateRequest request);

    void deleteRole(Long roleId);
}
