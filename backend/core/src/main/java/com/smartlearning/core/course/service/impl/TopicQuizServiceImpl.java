
package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.activity.LearningActivityRecorder;
import com.smartlearning.core.course.dto.request.*;
import com.smartlearning.core.course.dto.response.*;
import com.smartlearning.core.course.entity.*;
import com.smartlearning.core.course.entity.enums.*;
import com.smartlearning.core.course.repository.*;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.service.TopicQuizService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@RequiredArgsConstructor
public class TopicQuizServiceImpl implements TopicQuizService {

    private final TopicQuizRepository quizRepository;
    private final AssessmentAttemptRepository attemptRepository;
    private final CourseTopicRepository topicRepository;
    private final CourseAccessPolicy courseAccessPolicy;
    private final LearningActivityRecorder activityRecorder;

    public TopicQuizEditorResponse createDraft(
            UUID courseId, UUID topicId, UUID userId, TopicQuizCreateRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);
        CourseTopic topic = requireTopicForManagement(courseId, topicId);
        return saveDraft(topic, userId, request.title(), request.description(), request.questions());
    }

    public TopicQuizEditorResponse createNextVersion(
            UUID courseId, UUID topicId, UUID sourceId, UUID userId
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);
        CourseTopic topic = requireTopicForManagement(courseId, topicId);
        TopicQuiz source = requireQuiz(topicId, sourceId);

        if (source.getStatus() == TopicQuizStatus.DRAFT) {
            throw conflict("Bản nháp hiện tại có thể chỉnh sửa trực tiếp.");
        }

        List<TopicQuizQuestionRequest> questions = source.getAssessment().getQuestions().stream()
                .map(q -> new TopicQuizQuestionRequest(
                        q.getType(), q.getContent(), q.getExplanation(),
                        q.getOptions().stream()
                                .map(o -> new TopicQuizQuestionRequest.OptionRequest(
                                        o.getContent(), o.isCorrect()))
                                .toList()
                ))
                .toList();

        return saveDraft(topic, userId, source.getTitle(), source.getDescription(), questions);
    }

    public TopicQuizEditorResponse updateQuestions(
            UUID courseId, UUID topicId, UUID quizId,
            UUID userId, TopicQuizQuestionsRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);
        requireTopic(courseId, topicId);
        TopicQuiz quiz = requireQuizForUpdate(topicId, quizId);

        if (quiz.getStatus() == TopicQuizStatus.ARCHIVED) {
            throw conflict("Không thể sửa câu hỏi của phiên bản đã lưu trữ.");
        }

        if (attemptRepository.existsByAssessmentId(quizId)) {
            throw conflict("Đề đã có bài nộp. Hãy tạo phiên bản mới để sửa câu hỏi hoặc đáp án.");
        }

        validateQuestions(request.questions());
        replaceQuestions(quiz.getAssessment(), request.questions());
        quizRepository.flush();

        return toEditorResponse(quiz, false);
    }

    public TopicQuizEditorResponse updateDetails(
            UUID courseId, UUID topicId, UUID quizId,
            UUID userId, TopicQuizDetailsRequest request
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);
        requireTopic(courseId, topicId);
        TopicQuiz quiz = requireQuizForUpdate(topicId, quizId);

        if (request.title() != null) {
            String title = request.title().trim();
            if (title.isEmpty() || title.length() > 255) {
                throw invalid("Tên bài kiểm tra phải có từ 1 đến 255 ký tự.");
            }
            quiz.setTitle(title);
        }

        if (request.description() != null) {
            quiz.setDescription(normalize(request.description()));
        }

        if (request.explanations() != null) {
            Map<UUID, AssessmentQuestion> byId = quiz.getAssessment().getQuestions().stream()
                    .collect(Collectors.toMap(AssessmentQuestion::getId, q -> q));
            Set<UUID> updated = new HashSet<>();

            for (TopicQuizDetailsRequest.ExplanationUpdate input : request.explanations()) {
                if (input == null || input.questionId() == null || !updated.add(input.questionId())) {
                    throw invalid("Danh sách lời giải không hợp lệ hoặc bị trùng.");
                }

                AssessmentQuestion question = byId.get(input.questionId());
                if (question == null) throw invalid("Câu hỏi không thuộc bài kiểm tra.");
                question.setExplanation(normalize(input.explanation()));
            }
        }

        quizRepository.flush();
        return toEditorResponse(quiz, attemptRepository.existsByAssessmentId(quizId));
    }

    public TopicQuizEditorResponse publish(
            UUID courseId, UUID topicId, UUID quizId, UUID userId
    ) {
        courseAccessPolicy.requireOwner(courseId, userId);
        CourseTopic topic = requireTopicForManagement(courseId, topicId);
        TopicQuiz quiz = requireQuizForUpdate(topicId, quizId);

        if (quiz.getStatus() != TopicQuizStatus.DRAFT) {
            throw conflict("Chỉ có thể xuất bản bản nháp.");
        }

        if (topic.getChapter().getCourse().getStatus() != CourseStatus.PUBLISHED) {
            throw conflict("Khóa học chưa được xuất bản.");
        }

        TopicQuiz current = quizRepository.findByTopicIdAndStatus(
                topicId, TopicQuizStatus.PUBLISHED
        ).orElse(null);

        if (current != null) {
            current = requireQuizForUpdate(topicId, current.getId());
            current.setStatus(TopicQuizStatus.ARCHIVED);
            quizRepository.flush();
        }

        quiz.setStatus(TopicQuizStatus.PUBLISHED);
        quiz.setPublishedAt(Instant.now());
        quizRepository.flush();

        return toEditorResponse(quiz, false);
    }

    @Transactional(readOnly = true)
    public List<TopicQuizEditorResponse> getVersions(UUID courseId, UUID topicId, UUID userId) {
        courseAccessPolicy.requireOwner(courseId, userId);
        requireTopic(courseId, topicId);

        List<TopicQuiz> versions = quizRepository.findAllByTopicIdOrderByVersionDesc(topicId);
        if (versions.isEmpty()) return List.of();

        List<UUID> ids = versions.stream().map(TopicQuiz::getId).toList();
        Set<UUID> attempted = new HashSet<>(attemptRepository.findAttemptedAssessmentIds(ids));

        return versions.stream()
                .map(quiz -> toEditorResponse(quiz, attempted.contains(quiz.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicQuizStudentResponse getCurrent(UUID courseId, UUID topicId, UUID userId) {
        requireStudent(courseId, userId);
        requireTopic(courseId, topicId);

        return quizRepository.findByTopicIdAndStatus(topicId, TopicQuizStatus.PUBLISHED)
                .map(this::toStudentResponse)
                .orElse(null);
    }

    public TopicQuizAttemptResponse submit(
            UUID courseId, UUID topicId, UUID quizId,
            UUID userId, TopicQuizSubmitRequest request
    ) {
        requireStudent(courseId, userId);
        CourseTopic topic = requireTopic(courseId, topicId);

        TopicQuiz quiz = quizRepository.findForSubmission(topicId, quizId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        if (quiz.getStatus() != TopicQuizStatus.PUBLISHED ||
                topic.getChapter().getCourse().getStatus() != CourseStatus.PUBLISHED) {
            throw conflict("Đề kiểm tra đã thay đổi. Vui lòng tải lại.");
        }

        Map<UUID, List<UUID>> submitted = readAnswers(request);
        List<AssessmentQuestion> questions = quiz.getAssessment().getQuestions();
        Set<UUID> questionIds = questions.stream()
                .map(AssessmentQuestion::getId)
                .collect(Collectors.toSet());

        if (!questionIds.equals(submitted.keySet())) {
            throw invalid("Đề kiểm tra đã thay đổi. Vui lòng tải lại trước khi nộp.");
        }

        AssessmentAttempt attempt = new AssessmentAttempt();
        attempt.setAssessment(quiz.getAssessment());
        attempt.setStudentId(userId);
        attempt.setMaxScore(questions.size());
        attempt.setSubmittedAt(Instant.now());

        int totalScore = 0;

        for (AssessmentQuestion question : questions) {
            List<UUID> selected = submitted.get(question.getId());
            validateSelectedOptions(question, selected);

            int score = gradeQuestion(question, selected);
            totalScore += score;

            AttemptAnswer answer = new AttemptAnswer();
            answer.setAttempt(attempt);
            answer.setQuestion(question);
            answer.setSelectedOptionIds(new ArrayList<>(selected));
            answer.setAwardedScore(score);
            attempt.getAnswers().add(answer);
        }

        attempt.setScore(totalScore);
        AssessmentAttempt saved = attemptRepository.saveAndFlush(attempt);

        activityRecorder.recordTopicQuizSubmitted(
                userId, courseId, topicId, quizId,
                saved.getId(), totalScore, saved.getMaxScore()
        );

        return toAttemptResponse(saved, quiz);
    }

    @Transactional(readOnly = true)
    public List<TopicQuizAttemptSummaryResponse> getHistory(
            UUID courseId, UUID topicId, UUID userId
    ) {
        requireStudent(courseId, userId);
        requireTopic(courseId, topicId);

        List<TopicQuiz> versions = quizRepository.findAllByTopicIdOrderByVersionDesc(topicId);
        if (versions.isEmpty()) return List.of();

        Map<UUID, Integer> versionById = versions.stream()
                .collect(Collectors.toMap(TopicQuiz::getId, TopicQuiz::getVersion));

        return attemptRepository.findHistory(versionById.keySet(), userId).stream()
                .map(a -> new TopicQuizAttemptSummaryResponse(
                        a.getId(), a.getAssessment().getId(),
                        versionById.get(a.getAssessment().getId()),
                        a.getScore(), a.getMaxScore(), a.getSubmittedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public TopicQuizAttemptResponse getAttempt(
            UUID courseId, UUID topicId, UUID attemptId, UUID userId
    ) {
        requireStudent(courseId, userId);
        requireTopic(courseId, topicId);

        AssessmentAttempt attempt = attemptRepository.findByIdAndStudentId(attemptId, userId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));

        TopicQuiz quiz = requireQuiz(topicId, attempt.getAssessment().getId());
        return toAttemptResponse(attempt, quiz);
    }

    private TopicQuizEditorResponse saveDraft(
            CourseTopic topic, UUID userId, String title, String description,
            List<TopicQuizQuestionRequest> questions
    ) {
        validateQuestions(questions);

        Assessment assessment = new Assessment();
        assessment.setType(AssessmentType.TOPIC_QUIZ);
        replaceQuestions(assessment, questions);

        TopicQuiz quiz = new TopicQuiz();
        quiz.setAssessment(assessment);
        quiz.setTopic(topic);
        quiz.setTitle(title.trim());
        quiz.setDescription(normalize(description));
        quiz.setVersion(quizRepository.findMaxVersion(topic.getId()) + 1);
        quiz.setCreatedBy(userId);

        return toEditorResponse(quizRepository.saveAndFlush(quiz), false);
    }

    private void replaceQuestions(Assessment assessment, List<TopicQuizQuestionRequest> inputs) {
        assessment.getQuestions().clear();

        for (int i = 0; i < inputs.size(); i++) {
            TopicQuizQuestionRequest input = inputs.get(i);

            AssessmentQuestion question = new AssessmentQuestion();
            question.setAssessment(assessment);
            question.setType(input.type());
            question.setContent(input.content().trim());
            question.setExplanation(normalize(input.explanation()));

            for (int j = 0; j < input.options().size(); j++) {
                TopicQuizQuestionRequest.OptionRequest optionInput = input.options().get(j);
                QuestionOption option = new QuestionOption();
                option.setQuestion(question);
                option.setContent(optionInput.content().trim());
                option.setCorrect(optionInput.correct());
                question.getOptions().add(option);
            }

            assessment.getQuestions().add(question);
        }
    }

    private void validateQuestions(List<TopicQuizQuestionRequest> questions) {
        if (questions == null || questions.isEmpty()) {
            throw invalid("Bài kiểm tra phải có ít nhất một câu hỏi.");
        }

        for (TopicQuizQuestionRequest question : questions) {
            if (question == null || question.type() == null ||
                    question.content() == null || question.content().isBlank() ||
                    question.options() == null || question.options().size() < 2) {
                throw invalid("Mỗi câu hỏi cần nội dung và ít nhất hai phương án.");
            }

            long correctCount = question.options().stream().filter(option -> {
                if (option == null || option.content() == null || option.content().isBlank()) {
                    throw invalid("Nội dung phương án không được để trống.");
                }
                return option.correct();
            }).count();

            if (question.type() == QuestionType.SINGLE_CHOICE && correctCount != 1) {
                throw invalid("Câu hỏi một đáp án phải có đúng một phương án đúng.");
            }

            if (question.type() == QuestionType.MULTIPLE_CHOICE && correctCount < 2) {
                throw invalid("Câu hỏi nhiều đáp án phải có ít nhất hai phương án đúng.");
            }
        }
    }

    private Map<UUID, List<UUID>> readAnswers(TopicQuizSubmitRequest request) {
        if (request == null || request.answers() == null) {
            throw invalid("Danh sách câu trả lời không hợp lệ.");
        }

        Map<UUID, List<UUID>> answers = new HashMap<>();

        for (TopicQuizSubmitRequest.Answer answer : request.answers()) {
            if (answer == null || answer.questionId() == null || answer.selectedOptionIds() == null) {
                throw invalid("Câu trả lời không hợp lệ.");
            }

            if (answers.putIfAbsent(answer.questionId(), answer.selectedOptionIds()) != null) {
                throw invalid("Không được gửi trùng câu hỏi.");
            }
        }

        return answers;
    }

    private void validateSelectedOptions(AssessmentQuestion question, List<UUID> selected) {
        if (selected.contains(null) || new HashSet<>(selected).size() != selected.size()) {
            throw invalid("Phương án được chọn không hợp lệ hoặc bị trùng.");
        }

        if (question.getType() == QuestionType.SINGLE_CHOICE && selected.size() > 1) {
            throw invalid("Câu hỏi một đáp án chỉ cho phép chọn tối đa một phương án.");
        }

        Set<UUID> validIds = question.getOptions().stream()
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());

        if (!validIds.containsAll(selected)) {
            throw invalid("Phương án không thuộc câu hỏi. Vui lòng tải lại đề.");
        }
    }

    static int gradeQuestion(AssessmentQuestion question, List<UUID> selected) {
        Set<UUID> correctIds = question.getOptions().stream()
                .filter(QuestionOption::isCorrect)
                .map(QuestionOption::getId)
                .collect(Collectors.toSet());

        return new HashSet<>(selected).equals(correctIds) ? 1 : 0;
    }

    private TopicQuizEditorResponse toEditorResponse(TopicQuiz quiz, boolean hasAttempts) {
        return new TopicQuizEditorResponse(
                quiz.getId(), quiz.getTopic().getId(),
                quiz.getTitle(), quiz.getDescription(), quiz.getVersion(),
                quiz.getStatus(), hasAttempts, quiz.getPublishedAt(),
                quiz.getAssessment().getQuestions().stream()
                        .map(q -> new TopicQuizEditorResponse.Question(
                                q.getId(), q.getType(), q.getContent(), q.getExplanation(),
                                q.getOptions().stream()
                                        .map(o -> new TopicQuizEditorResponse.Option(
                                                o.getId(), o.getContent(), o.isCorrect()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private TopicQuizStudentResponse toStudentResponse(TopicQuiz quiz) {
        return new TopicQuizStudentResponse(
                quiz.getId(), quiz.getTopic().getId(),
                quiz.getTitle(), quiz.getDescription(), quiz.getVersion(),
                quiz.getAssessment().getQuestions().stream()
                        .map(q -> new TopicQuizStudentResponse.Question(
                                q.getId(), q.getType(), q.getContent(),
                                q.getOptions().stream()
                                        .map(o -> new TopicQuizStudentResponse.Option(
                                                o.getId(), o.getContent()
                                        ))
                                        .toList()
                        ))
                        .toList()
        );
    }

    private TopicQuizAttemptResponse toAttemptResponse(AssessmentAttempt attempt, TopicQuiz quiz) {
        Map<UUID, AttemptAnswer> savedAnswers = attempt.getAnswers().stream()
                .collect(Collectors.toMap(a -> a.getQuestion().getId(), a -> a));

        List<TopicQuizAttemptResponse.AnswerReview> reviews = quiz.getAssessment().getQuestions().stream()
                .map(q -> {
                    AttemptAnswer answer = savedAnswers.get(q.getId());
                    List<UUID> correctIds = q.getOptions().stream()
                            .filter(QuestionOption::isCorrect)
                            .map(QuestionOption::getId)
                            .toList();

                    return new TopicQuizAttemptResponse.AnswerReview(
                            q.getId(), q.getType(), q.getContent(),
                            q.getOptions().stream()
                                    .map(o -> new TopicQuizAttemptResponse.Option(
                                            o.getId(), o.getContent()
                                    ))
                                    .toList(),
                            answer == null ? List.of() : answer.getSelectedOptionIds(),
                            correctIds,
                            answer == null ? 0 : answer.getAwardedScore(),
                            q.getExplanation()
                    );
                })
                .toList();

        return new TopicQuizAttemptResponse(
                attempt.getId(), quiz.getId(), quiz.getVersion(),
                attempt.getScore(), attempt.getMaxScore(), attempt.getSubmittedAt(), reviews
        );
    }

    private CourseTopic requireTopic(UUID courseId, UUID topicId) {
        return topicRepository.findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                topicId, courseId
        ).orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private CourseTopic requireTopicForManagement(UUID courseId, UUID topicId) {
        return topicRepository.findForQuizManagement(courseId, topicId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private TopicQuiz requireQuiz(UUID topicId, UUID quizId) {
        return quizRepository.findByIdAndTopicId(quizId, topicId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private TopicQuiz requireQuizForUpdate(UUID topicId, UUID quizId) {
        return quizRepository.findForUpdate(topicId, quizId)
                .orElseThrow(() -> new ApplicationException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    private void requireStudent(UUID courseId, UUID userId) {
        CourseMember member = courseAccessPolicy.requireActiveMember(courseId, userId);
        if (member == null || member.getRole() != CourseMemberRole.STUDENT) {
            throw new ApplicationException(CommonErrorCode.FORBIDDEN);
        }
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ApplicationException invalid(String message) {
        return new ApplicationException(CommonErrorCode.VALIDATION_FAILED, message);
    }

    private ApplicationException conflict(String message) {
        return new ApplicationException(CommonErrorCode.DATA_CONFLICT, message);
    }
}

