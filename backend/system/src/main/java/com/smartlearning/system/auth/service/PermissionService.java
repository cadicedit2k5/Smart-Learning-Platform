package com.smartlearning.system.auth.service;

import com.smartlearning.system.auth.dto.response.PermissionResponse;
import com.smartlearning.system.auth.entity.Permission;

import java.util.List;

public interface PermissionService {
    List<PermissionResponse> getPermissions();
}
