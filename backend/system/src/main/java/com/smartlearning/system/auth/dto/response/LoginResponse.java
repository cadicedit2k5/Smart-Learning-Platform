package com.smartlearning.system.auth.dto.response;

public record LoginResponse(
        String accessToken,
        long expiresIn
) {
}