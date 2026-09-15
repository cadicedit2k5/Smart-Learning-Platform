package com.smartlearning.core.course.dto.request;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

public record AssignmentSubmissionRequest(

        @Size(max = 20000)
        String content,

        MultipartFile file
) {
}
