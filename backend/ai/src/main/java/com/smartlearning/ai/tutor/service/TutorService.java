package com.smartlearning.ai.tutor.service;

import com.smartlearning.ai.tutor.dto.response.TutorAnswerResponse;
import java.util.UUID;

public interface TutorService {
    TutorAnswerResponse ask(UUID courseId, String accessToken, String question);

}
