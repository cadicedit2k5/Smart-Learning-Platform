package com.smartlearning.core.course.activity;

import com.smartlearning.core.course.entity.LearningActivity;
import com.smartlearning.core.course.entity.enums.LearningActivityType;
import com.smartlearning.core.course.repository.LearningActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.json.JsonMapper;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.TOPIC_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LearningActivityRecorderTest {

    private LearningActivityRepository repository;
    private LearningActivityRecorder recorder;

    @BeforeEach
    void setUp() {
        repository = mock(LearningActivityRepository.class);
        recorder = new LearningActivityRecorder(repository, JsonMapper.builder().build());
    }

    @Test
    void recordTopicCompleted_persistsExpectedActivity() {
        recorder.recordTopicCompleted(STUDENT_ID, COURSE_ID, TOPIC_ID, 1200);

        ArgumentCaptor<LearningActivity> captor = ArgumentCaptor.forClass(LearningActivity.class);
        verify(repository).save(captor.capture());

        LearningActivity activity = captor.getValue();

        assertThat(activity.getUserId()).isEqualTo(STUDENT_ID);
        assertThat(activity.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(activity.getEventType()).isEqualTo(LearningActivityType.TOPIC_COMPLETED);
        assertThat(activity.getOccurredAt()).isNotNull();
        assertThat(activity.getData().get("topicId")).isEqualTo(TOPIC_ID.toString());
        assertThat(activity.getData().get("activeSeconds")).isEqualTo(1200);
    }
}