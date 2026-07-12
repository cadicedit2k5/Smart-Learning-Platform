package com.smartlearning.system.user.service.impl;

import com.smartlearning.system.user.entity.Role;
import com.smartlearning.system.user.repository.RoleRepository;
import com.smartlearning.system.user.service.RoleService;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceIml implements RoleService {
    private RoleRepository roleRepository;

    public RoleServiceIml(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role handleGetRoleByCode(String code) {
        return this.roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        "Role STUDENT chưa được khởi tạo"
                ));
    }
}
