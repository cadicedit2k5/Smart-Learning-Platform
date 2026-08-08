package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record AccessCodeCreateRequest(

        @Future(message = "Thời điểm hết hạn phải ở tương lai")
        Instant expiresAt
) {
}