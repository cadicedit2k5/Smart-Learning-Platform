package com.smartlearning.system.user.service;

import com.smartlearning.system.user.entity.Role;

public interface RoleService {
    Role handleGetRoleByCode(String code);
}
