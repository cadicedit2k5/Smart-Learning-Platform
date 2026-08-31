package com.smartlearning.system.auth.service.impl;

import com.smartlearning.system.auth.entity.Role;
import com.smartlearning.system.auth.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleServiceImlTest {

    @Mock
    private RoleRepository roleRepository;

    @InjectMocks
    private RoleServiceImpl roleService;

    @Test
    void handleGetRoleByCode_returnsRoleWhenItExists() {
        Role role = new Role();
        role.setCode("STUDENT");
        when(roleRepository.findByCode("STUDENT"))
                .thenReturn(Optional.of(role));

        Role result = roleService.handleGetRoleByCode("STUDENT");

        assertThat(result).isSameAs(role);
    }

    @Test
    void handleGetRoleByCode_throwsWhenRoleHasNotBeenInitialized() {
        when(roleRepository.findByCode("STUDENT"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> roleService.handleGetRoleByCode("STUDENT")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("STUDENT");
    }
}
