package com.smartlearning.system.auth.dto.response;

import com.smartlearning.system.auth.entity.Role;

public record RoleResponse(
        Long id,
        String code,
        String name
) {
    public static RoleResponse from(Role role) {
        if (role == null) {
            return null;
        }

        return new RoleResponse(
                role.getId(),
                role.getCode(),
                role.getName()
        );
    }
}
