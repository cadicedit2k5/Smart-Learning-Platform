package com.smartlearning.ai.tutor.service.impl;

import com.smartlearning.ai.infrastructure.http.CoreCourseAccessClient;
import com.smartlearning.ai.infrastructure.http.PythonAiEngineClient;
import com.smartlearning.ai.tutor.dto.response.TutorAnswerResponse;
import com.smartlearning.ai.tutor.service.TutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TutorServiceImpl implements TutorService {

    private final CoreCourseAccessClient courseAccessClient;
    private final PythonAiEngineClient aiEngineClient;

    public TutorAnswerResponse ask(UUID courseId, String accessToken, String question) {

        courseAccessClient.requireActiveMember(courseId, accessToken);

        PythonAiEngineClient.RagResult result = aiEngineClient.answer(courseId, question.trim());

        return new TutorAnswerResponse(
            result.answer(),
            result.citations().stream() .map(citation ->
                new TutorAnswerResponse.Citation(
                        citation.label(),
                        citation.chunkId(),
                        citation.documentId(),
                        citation.documentVersionId(),
                        citation.locator()
                )).toList()
        );
    }
}
