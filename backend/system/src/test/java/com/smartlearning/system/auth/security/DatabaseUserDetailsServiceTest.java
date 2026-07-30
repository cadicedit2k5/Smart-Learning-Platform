package com.smartlearning.system.auth.security;

import com.smartlearning.system.auth.entity.Permission;
import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.entity.User;
import com.smartlearning.system.auth.entity.enums.UserStatus;
import com.smartlearning.system.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DatabaseUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DatabaseUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_normalizesEmailAndBuildsAuthorities() {
        UUID userId = UUID.randomUUID();
        Permission readPermission = permission("COURSE_READ");
        Permission createPermission = permission("COURSE_CREATE");
        Role role = new Role();
        role.setCode("student");
        role.setPermissions(Set.of(readPermission, createPermission));
        User user = new User();
        user.setId(userId);
        user.setEmail("student@example.com");
        user.setPassword("encoded-password");
        user.setRole(role);

        when(userRepository.findByEmailIgnoreCaseAndStatus(
                "student@example.com",
                UserStatus.ACTIVE
        )).thenReturn(Optional.of(user));

        SecurityUser result = (SecurityUser) userDetailsService
                .loadUserByUsername("  Student@Example.COM ");

        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.getUsername()).isEqualTo("student@example.com");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_STUDENT",
                        "COURSE_READ",
                        "COURSE_CREATE"
                );
    }

    @Test
    void loadUserByUsername_throwsWhenActiveUserDoesNotExist() {
        when(userRepository.findByEmailIgnoreCaseAndStatus(
                "missing@example.com",
                UserStatus.ACTIVE
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService
                .loadUserByUsername(" Missing@Example.COM "))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static Permission permission(String code) {
        Permission permission = new Permission();
        permission.setCode(code);
        return permission;
    }
}
