package com.smartlearning.core.course.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AccessCodeUtils {
    public String generateRawCode() {
        String randomPart = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();

        return "COURSE-" + randomPart;
    }
}
