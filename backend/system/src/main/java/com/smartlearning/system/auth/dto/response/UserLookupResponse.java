package com.smartlearning.system.auth.dto.response;

import com.smartlearning.system.auth.entity.User;

import java.util.UUID;

public record UserLookupResponse(
        UUID id,
        String email,
        String fullName,
        String avatar,
        RoleResponse role
) {
    public static UserLookupResponse from(User user) {
        return new UserLookupResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getAvatar(),
                RoleResponse.from(user.getRole())
        );
    }
}
