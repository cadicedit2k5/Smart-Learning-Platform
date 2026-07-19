package com.smartlearning.system.auth.service.impl;

import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.repository.RoleRepository;
import com.smartlearning.system.auth.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoleServiceIml implements RoleService {
    private final RoleRepository roleRepository;

    @Override
    public Role handleGetRoleByCode(String code) {
        return this.roleRepository.findByCode(code)
                .orElseThrow(() -> new IllegalStateException(
                        String.format("Role %s chưa được khởi tạo", code)
                ));
    }
}
