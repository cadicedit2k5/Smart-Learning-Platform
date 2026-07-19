package com.smartlearning.system.auth.service;

import com.smartlearning.system.auth.entity.Role;

public interface RoleService {
    Role handleGetRoleByCode(String code);
}
