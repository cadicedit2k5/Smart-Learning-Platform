package com.smartlearning.system.auth.repository;

import com.smartlearning.system.auth.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);

    List<Permission> findAllByOrderByCodeAsc();
}
