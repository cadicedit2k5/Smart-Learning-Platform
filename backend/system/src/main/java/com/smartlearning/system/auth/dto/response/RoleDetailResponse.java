package com.smartlearning.system.auth.dto.response;

import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;

import java.util.Comparator;
import java.util.List;

public record RoleDetailResponse(
        Long id,
        String code,
        String name,
        List<PermissionResponse> permissions
) {

    public static RoleDetailResponse from(Role role) {
        if (role == null) {
            return null;
        }

        List<PermissionResponse> permissions = role.getPermissions().stream()
                        .sorted(Comparator.comparing(Permission::getCode))
                        .map(PermissionResponse::from).toList();

        return new RoleDetailResponse(
                role.getId(),
                role.getCode(),
                role.getName(),
                permissions
        );
    }
}
