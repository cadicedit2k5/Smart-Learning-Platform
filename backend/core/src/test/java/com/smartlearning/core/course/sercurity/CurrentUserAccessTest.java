package com.smartlearning.core.course.sercurity;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserAccessTest {

    private final CurrentUserAccess currentUserAccess = new CurrentUserAccess();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "authorities {0} => admin {1}")
    @MethodSource("authorityScenarios")
    void recognizesOnlyAdminRole(List<String> authorities, boolean expected) {
        authenticateWith(authorities);

        assertThat(currentUserAccess.isAdmin()).isEqualTo(expected);
    }

    @Test
    void unauthenticatedRequestIsNotAdmin() {
        assertThat(currentUserAccess.isAdmin()).isFalse();
    }

    static Stream<Arguments> authorityScenarios() {
        return Stream.of(
                Arguments.of(List.of("ROLE_ADMIN", "COURSE_READ"), true),
                Arguments.of(List.of("ROLE_LECTURER", "COURSE_MANAGE"), false),
                Arguments.of(List.of("ADMIN", "COURSE_READ"), false)
        );
    }

    private static void authenticateWith(List<String> authorities) {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        "user",
                        null,
                        authorities.stream()
                                .map(SimpleGrantedAuthority::new)
                                .toList()
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
