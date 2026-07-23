package com.smartlearning.system.configs;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.RoleRepository;
import com.smartlearning.system.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;


    @Value("${app.data-init.admin-email}")
    private String adminEmail;

    @Value("${app.data-init.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        createRoleIfNotExists("STUDENT", "Student");
        createRoleIfNotExists("LECTURER", "Lecturer");
        createRoleIfNotExists("ADMIN", "Administrator");
        createAdminIfNotExists(roleRepository.findByCode("ADMIN")
                .orElseThrow(() ->
                        new ApplicationException(
                                CommonErrorCode.RESOURCE_NOT_FOUND,
                                "ROLE ADMIN chưa được khởi tạo!")));
    }

    private void createRoleIfNotExists(String code, String name) {
        if (!roleRepository.existsByCode(code)) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
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