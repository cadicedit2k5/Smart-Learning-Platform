package com.smartlearning.system.auth.repository;

import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID>,
        JpaSpecificationExecutor<User> {

    @EntityGraph(attributePaths = {
            "role",
            "role.permissions"
    })
    Optional<User> findByEmailIgnoreCaseAndStatus(String email, UserStatus status);

    Optional<User> findByIdAndStatusNot(UUID id, UserStatus status);

    List<User> findAllByIdInAndStatus(Collection<UUID> ids, UserStatus status);

    boolean existsByEmailIgnoreCase(String email);
}
