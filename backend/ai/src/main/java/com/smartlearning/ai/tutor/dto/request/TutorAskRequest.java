package com.smartlearning.ai.tutor.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TutorAskRequest(

        @NotBlank(message = "Câu hỏi không được để trống")
        String question
) {
}