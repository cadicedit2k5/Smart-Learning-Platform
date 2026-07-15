package com.smartlearning.system.user.dto.response;

import com.smartlearning.system.user.entity.Role;
import com.smartlearning.system.user.entity.User;

import java.time.Instant;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String avatar,
        Role role,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}