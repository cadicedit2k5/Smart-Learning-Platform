
package com.smartlearning.core.course.service;

import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.*;

import java.util.List;
import java.util.UUID;

public interface TopicQuizService {

    TopicQuizEditorResponse createDraft(UUID courseId, UUID topicId, UUID userId, TopicQuizCreateRequest request);

    TopicQuizEditorResponse createNextVersion(UUID courseId, UUID topicId, UUID sourceId, UUID userId);

    TopicQuizEditorResponse updateQuestions(UUID courseId, UUID topicId, UUID quizId, UUID userId, TopicQuizQuestionsRequest request);

    TopicQuizEditorResponse updateDetails(UUID courseId, UUID topicId, UUID quizId, UUID userId, TopicQuizDetailsRequest request);

    TopicQuizEditorResponse publish(UUID courseId, UUID topicId, UUID quizId, UUID userId);

    List<TopicQuizEditorResponse> getVersions(UUID courseId, UUID topicId, UUID userId);

    TopicQuizStudentResponse getCurrent(UUID courseId, UUID topicId, UUID userId);

    TopicQuizAttemptResponse submit(UUID courseId, UUID topicId, UUID quizId, UUID userId, TopicQuizSubmitRequest request);

    List<TopicQuizAttemptSummaryResponse> getHistory(UUID courseId, UUID topicId, UUID userId);

    TopicQuizAttemptResponse getAttempt(UUID courseId, UUID topicId, UUID attemptId, UUID userId);
}
