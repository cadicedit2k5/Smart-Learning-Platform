package com.smartlearning.system.auth.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Set;
import java.util.UUID;

public record UserBatchLookupRequest(

        @NotEmpty(message = "Danh sách người dùng không được để trống")
        Set<@NotNull UUID> userIds

) {
}