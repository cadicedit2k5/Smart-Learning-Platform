package com.smartlearning.system.auth.repository;

import com.smartlearning.system.auth.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByCode(String code);

    @EntityGraph(attributePaths = "permissions")
    @Query("""
            select distinct role
            from Role role
            """)
    List<Role> findAllWithPermissions();

    @EntityGraph(attributePaths = "permissions")
    @Query("""
            select role
            from Role role
            where role.id = :id
            """)
    Optional<Role> findByIdWithPermissions(
            @Param("id") Long id
    );
}
