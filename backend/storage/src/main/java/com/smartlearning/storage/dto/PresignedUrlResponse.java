package com.smartlearning.storage.dto;

import java.time.Instant;

public record PresignedUrlResponse(
        String url,
        Instant expiresAt
) {
}
