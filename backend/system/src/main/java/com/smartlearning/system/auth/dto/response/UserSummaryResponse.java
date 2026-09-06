package com.smartlearning.system.auth.dto.response;

import com.smartlearning.system.auth.entity.User;

import java.util.UUID;

public record UserSummaryResponse(
        UUID id,
        String email,
        String fullName
) {

    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }
}
