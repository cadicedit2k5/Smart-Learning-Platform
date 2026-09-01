package com.smartlearning.system.support;

import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;

import java.util.Arrays;
import java.util.HashSet;

public final class SystemTestData {

    public static final long STUDENT_ROLE_ID = 10L;
    public static final long ADMIN_ROLE_ID = 11L;
    public static final long COURSE_READ_PERMISSION_ID = 100L;
    public static final long COURSE_WRITE_PERMISSION_ID = 101L;

    private SystemTestData() {
    }

    public static Permission permission(long id, String code) {
        Permission permission = new Permission();
        permission.setId(id);
        permission.setCode(code);
        permission.setDescription(code + " description");
        return permission;
    }

    public static Role role(long id, String code, Permission... permissions) {
        Role role = new Role();
        role.setId(id);
        role.setCode(code);
        role.setName(code + " role");
        role.setPermissions(new HashSet<>(Arrays.asList(permissions)));
        return role;
    }
}
