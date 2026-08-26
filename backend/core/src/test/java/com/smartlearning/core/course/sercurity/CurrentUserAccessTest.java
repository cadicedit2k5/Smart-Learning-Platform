package com.smartlearning.core.course.sercurity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserAccessTest {

    private final CurrentUserAccess currentUserAccess = new CurrentUserAccess();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recognizesAdminRole() {
        authenticateWith("ROLE_ADMIN", "COURSE_READ");

        assertThat(currentUserAccess.isAdmin()).isTrue();
    }

    @Test
    void doesNotTreatCoursePermissionAsAdminRole() {
        authenticateWith("ROLE_LECTURER", "COURSE_MANAGE", "COURSE_READ");

        assertThat(currentUserAccess.isAdmin()).isFalse();
    }

    @Test
    void unauthenticatedRequestIsNotAdmin() {
        assertThat(currentUserAccess.isAdmin()).isFalse();
    }

    private static void authenticateWith(String... authorities) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        List.of(authorities)
                                .stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
