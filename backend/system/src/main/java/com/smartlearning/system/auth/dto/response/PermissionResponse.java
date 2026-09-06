package com.smartlearning.system.auth.dto.response;

import com.smartlearning.system.auth.entity.Permission;

public record PermissionResponse(
        Long id,
        String code,
        String description
) {

    public static PermissionResponse from(Permission permission) {
        if (permission == null) {
            return null;
        }

        return new PermissionResponse(
                permission.getId(),
                permission.getCode(),
                permission.getDescription()
        );
    }
}
