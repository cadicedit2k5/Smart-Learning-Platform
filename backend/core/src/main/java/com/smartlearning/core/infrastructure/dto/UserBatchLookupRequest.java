package com.smartlearning.core.infrastructure.dto;

import java.util.Set;
import java.util.UUID;

public record UserBatchLookupRequest(
        Set<UUID> userIds
) {
}
