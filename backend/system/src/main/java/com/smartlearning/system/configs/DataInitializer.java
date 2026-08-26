package com.smartlearning.system.configs;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.PermissionRepository;
import com.smartlearning.system.auth.repository.RoleRepository;
import com.smartlearning.system.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissionRepository permissionRepository;


    @Value("${app.data-init.admin-email}")
    private String adminEmail;

    @Value("${app.data-init.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        // Khời tạo Permission mặc định
        Permission courseManage = createPermissionIfNotExists("COURSE_MANAGE", "Quản lý môn học");
        Permission courseRead = createPermissionIfNotExists("COURSE_READ", "Truy cập môn học");
        Permission userRead = createPermissionIfNotExists("USER_READ", "Tra cứu người dùng");

        // Khởi tạo Role mặc định
        Role studentRole = createRoleIfNotExists("STUDENT", "Student");
        Role lecturerRole = createRoleIfNotExists("LECTURER", "Lecturer");
        Role adminRole = createRoleIfNotExists("ADMIN", "Administrator");

        // Gán quyền cho Role
        assignPermission(studentRole, courseRead);

        assignPermission(lecturerRole, courseManage, courseRead, userRead);
        assignPermission(adminRole, courseManage, courseRead, userRead);

        createAdminIfNotExists(roleRepository.findByCode("ADMIN")
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND,
                                "ROLE ADMIN chưa được khởi tạo!")));
    }

    private Role createRoleIfNotExists(String code, String name) {
        return roleRepository.findByCode(code).orElseGet(() -> {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            return roleRepository.save(role);
        });
    }

    private Permission createPermissionIfNotExists(String code, String description) {
        return permissionRepository.findByCode(code).orElseGet(() -> {
            Permission permission = new Permission();
            permission.setCode(code);
            permission.setDescription(description);

            return permissionRepository.save(permission);
        });
    }

    private void assignPermission(Role role, Permission ...permissions) {
        boolean changed = false;

        for (Permission permission : permissions) {
            boolean alreadyAssigned = role
                    .getPermissions()
                    .stream()
                    .anyMatch(existing ->
                            existing.getCode().equalsIgnoreCase(
                                    permission.getCode()
                            )
                    );

            if (!alreadyAssigned) {
                role.getPermissions().add(permission);
                changed = true;
            }
        }

        if (changed) {
            roleRepository.save(role);
        }
    }

    private void createAdminIfNotExists(Role adminRole) {
        String normalizedEmail = adminEmail
                .trim()
                .toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            return;
        }

        User admin = new User();
        admin.setEmail(normalizedEmail);
        admin.setPassword(
                passwordEncoder.encode(adminPassword)
        );
        admin.setFullName("Administrator");
        admin.setRole(adminRole);
        admin.setStatus(UserStatus.ACTIVE);

        userRepository.save(admin);
    }
}
