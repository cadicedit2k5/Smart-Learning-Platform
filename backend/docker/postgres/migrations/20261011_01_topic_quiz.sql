CREATE TABLE core.assessment (
                                 id UUID PRIMARY KEY,
                                 type VARCHAR(30) NOT NULL,
                                 created_at TIMESTAMPTZ NOT NULL,
                                 CONSTRAINT ck_assessment_type
                                     CHECK (type IN ('TOPIC_QUIZ'))
);

CREATE TABLE core.topic_quiz (
                                 id UUID PRIMARY KEY REFERENCES core.assessment(id),
                                 topic_id UUID NOT NULL REFERENCES core.course_topic(id),
                                 title VARCHAR(255) NOT NULL,
                                 description TEXT,
                                 version INT NOT NULL,
                                 status VARCHAR(20) NOT NULL,
                                 created_by UUID NOT NULL,
                                 published_at TIMESTAMPTZ,
                                 CONSTRAINT uk_topic_quiz_version UNIQUE (topic_id, version),
                                 CONSTRAINT ck_topic_quiz_version CHECK (version > 0),
                                 CONSTRAINT ck_topic_quiz_status CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED'))
);

CREATE UNIQUE INDEX uk_topic_quiz_published
    ON core.topic_quiz(topic_id)
    WHERE status = 'PUBLISHED';

CREATE TABLE core.assessment_question (
                                          id UUID PRIMARY KEY,
                                          assessment_id UUID NOT NULL REFERENCES core.assessment(id),
                                          type VARCHAR(30) NOT NULL,
                                          content TEXT NOT NULL,
                                          explanation TEXT,
                                          CONSTRAINT ck_assessment_question_type
                                              CHECK (type IN ('SINGLE_CHOICE', 'MULTIPLE_CHOICE'))
);

CREATE INDEX idx_assessment_question_assessment
    ON core.assessment_question(assessment_id);

CREATE TABLE core.question_option (
                                      id UUID PRIMARY KEY,
                                      question_id UUID NOT NULL REFERENCES core.assessment_question(id),
                                      content TEXT NOT NULL,
                                      is_correct BOOLEAN NOT NULL
);

CREATE INDEX idx_question_option_question
    ON core.question_option(question_id);

CREATE TABLE core.assessment_attempt (
                                         id UUID PRIMARY KEY,
                                         assessment_id UUID NOT NULL REFERENCES core.assessment(id),
                                         student_id UUID NOT NULL,
                                         score INT NOT NULL,
                                         max_score INT NOT NULL,
                                         submitted_at TIMESTAMPTZ NOT NULL,
                                         CONSTRAINT ck_assessment_attempt_score CHECK (score >= 0),
                                         CONSTRAINT ck_assessment_attempt_max_score CHECK (max_score > 0)
);

CREATE INDEX idx_assessment_attempt_student
    ON core.assessment_attempt(assessment_id, student_id, submitted_at);

CREATE TABLE core.attempt_answer (
                                     id UUID PRIMARY KEY,
                                     attempt_id UUID NOT NULL REFERENCES core.assessment_attempt(id),
                                     question_id UUID NOT NULL REFERENCES core.assessment_question(id),
                                     selected_option_ids JSONB NOT NULL,
                                     awarded_score INT NOT NULL,
                                     CONSTRAINT uk_attempt_answer_question UNIQUE (attempt_id, question_id),
                                     CONSTRAINT ck_attempt_answer_score CHECK (awarded_score IN (0, 1)),
                                     CONSTRAINT ck_attempt_answer_options CHECK (jsonb_typeof(selected_option_ids) = 'array')
);

CREATE INDEX idx_attempt_answer_attempt
    ON core.attempt_answer(attempt_id);

ALTER TABLE core.learning_activity
DROP CONSTRAINT IF EXISTS learning_activity_event_type_check;

ALTER TABLE core.learning_activity
    ADD CONSTRAINT learning_activity_event_type_check
        CHECK (
            event_type IN (
                           'TOPIC_STARTED',
                           'TOPIC_STUDIED',
                           'TOPIC_COMPLETED',
                           'TOPIC_QUIZ_SUBMITTED',
                           'ASSIGNMENT_SUBMITTED',
                           'ASSIGNMENT_GRADED'
                )
            );
