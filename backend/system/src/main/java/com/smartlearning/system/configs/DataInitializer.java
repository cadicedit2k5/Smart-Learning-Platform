package com.smartlearning.system.configs;

import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public DataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {
        createRoleIfNotExists("STUDENT", "Student");
        createRoleIfNotExists("LECTURER", "Lecturer");
        createRoleIfNotExists("ADMIN", "Administrator");
    }

    private void createRoleIfNotExists(String code, String name) {
        if (!roleRepository.existsByCode(code)) {
            Role role = new Role();
            role.setCode(code);
            role.setName(name);
            roleRepository.save(role);
        }
    }
}