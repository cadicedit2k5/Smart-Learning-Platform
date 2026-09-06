package com.smartlearning.core.infrastructure.dto;

import java.util.UUID;

public record SystemUserResponse(
        UUID id,
        String email,
        String fullName
) {
}
