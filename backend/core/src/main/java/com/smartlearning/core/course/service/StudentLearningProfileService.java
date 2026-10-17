package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.response.StudentLearningProfileResponse;

import java.util.UUID;

public interface StudentLearningProfileService {

    StudentLearningProfileResponse getStudentLearningProfile(
            UUID courseId,
            UUID studentId
    );
}