package com.smartlearning.common.utils;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public class JwtUtils {
    public static UUID getUserId(Jwt jwt) {
        String subject = jwt.getSubject();

        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT không có subject"
            );
        }

        return UUID.fromString(subject);
    }
}
