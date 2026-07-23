package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;

import java.time.Instant;

public record AccessCodeCreateRequest(

        @Min(
                value = 1,
                message = "Số lượt sử dụng tối đa phải lớn hơn 0"
        )
        Integer maxUses,

        @Future(message = "Thời điểm hết hạn phải ở tương lai")
        Instant expiresAt
) {
}