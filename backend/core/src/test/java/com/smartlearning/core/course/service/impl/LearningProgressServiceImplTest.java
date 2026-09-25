package com.smartlearning.core.course.service.impl;

import com.smartlearning.core.course.dto.response.TopicLearningProgressResponse;
import com.smartlearning.core.course.entity.CourseMember;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.entity.LearningProgress;
import com.smartlearning.core.course.entity.enums.CourseMemberRole;
import com.smartlearning.core.course.entity.enums.CourseMemberStatus;
import com.smartlearning.core.course.entity.enums.LearningProgressStatus;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseMemberRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.repository.LearningProgressRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import com.smartlearning.core.infrastructure.http.SystemClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.TOPIC_ID;
import static com.smartlearning.core.support.CoreTestData.member;
import static com.smartlearning.core.support.CoreTestData.topic;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningProgressServiceImplTest {

    @Mock
    private LearningProgressRepository progressRepository;

    @Mock
    private CourseTopicRepository topicRepository;

    @Mock
    private CourseChapterRepository chapterRepository;

    @Mock
    private CourseMemberRepository memberRepository;

    @Mock
    private CourseAccessPolicy courseAccessPolicy;

    @Mock
    private CourseUtils courseUtils;

    @Mock
    private SystemClient systemClient;

    @InjectMocks
    private LearningProgressServiceImpl learningProgressService;

    @Test
    void startTopicActivity_doesNotIncreaseActiveTime() {
        CourseTopic topic = topic();
        LearningProgress progress = progress(topic, LearningProgressStatus.IN_PROGRESS, 120);

        mockStudentAccess(topic);
        when(progressRepository.findByUserIdAndTopicId(STUDENT_ID, TOPIC_ID))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        TopicLearningProgressResponse result =
                learningProgressService.startTopicActivity(COURSE_ID, TOPIC_ID, STUDENT_ID);

        assertThat(result.activeSeconds()).isEqualTo(120);
        assertThat(progress.getActiveSeconds()).isEqualTo(120);
    }

    @Test
    void recordActivity_addsAtMostOneHeartbeat() {
        CourseTopic topic = topic();
        LearningProgress progress = progress(topic, LearningProgressStatus.IN_PROGRESS, 120);
        progress.setLastAccessedAt(Instant.now().minusSeconds(60));

        mockStudentAccess(topic);
        when(progressRepository.findByUserIdAndTopicId(STUDENT_ID, TOPIC_ID))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        TopicLearningProgressResponse result =
                learningProgressService.recordActivity(COURSE_ID, TOPIC_ID, STUDENT_ID);

        assertThat(result.activeSeconds()).isEqualTo(150);
    }

    @Test
    void completeTopic_allowsImmediateCompletionWithoutActivity() {
        CourseTopic topic = topic();

        mockStudentAccess(topic);
        when(progressRepository.findByUserIdAndTopicId(STUDENT_ID, TOPIC_ID))
                .thenReturn(Optional.empty());
        when(progressRepository.save(any(LearningProgress.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopicLearningProgressResponse result =
                learningProgressService.completeTopic(COURSE_ID, TOPIC_ID, STUDENT_ID);

        assertThat(result.status()).isEqualTo(LearningProgressStatus.COMPLETED);
        assertThat(result.activeSeconds()).isZero();
        assertThat(result.completedAt()).isNotNull();
    }

    @Test
    void recordActivity_continuesTrackingCompletedTopic() {
        CourseTopic topic = topic();
        LearningProgress progress = progress(topic, LearningProgressStatus.COMPLETED, 300);
        progress.setCompletedAt(Instant.now().minusSeconds(300));
        progress.setLastAccessedAt(Instant.now().minusSeconds(60));

        mockStudentAccess(topic);
        when(progressRepository.findByUserIdAndTopicId(STUDENT_ID, TOPIC_ID))
                .thenReturn(Optional.of(progress));
        when(progressRepository.save(progress)).thenReturn(progress);

        TopicLearningProgressResponse result =
                learningProgressService.recordActivity(COURSE_ID, TOPIC_ID, STUDENT_ID);

        assertThat(result.status()).isEqualTo(LearningProgressStatus.COMPLETED);
        assertThat(result.activeSeconds()).isEqualTo(330);
    }

    private void mockStudentAccess(CourseTopic topic) {
        CourseMember student = member(CourseMemberRole.STUDENT, CourseMemberStatus.ACTIVE);

        when(courseAccessPolicy.requireActiveMember(COURSE_ID, STUDENT_ID)).thenReturn(student);
        when(topicRepository.findByIdAndChapterCourseIdAndDeletedAtIsNullAndChapterDeletedAtIsNull(
                TOPIC_ID,
                COURSE_ID
        )).thenReturn(Optional.of(topic));
    }

    private LearningProgress progress(
            CourseTopic topic,
            LearningProgressStatus status,
            long activeSeconds
    ) {
        Instant now = Instant.now();

        LearningProgress progress = new LearningProgress();
        progress.setUserId(STUDENT_ID);
        progress.setTopic(topic);
        progress.setStatus(status);
        progress.setActiveSeconds(activeSeconds);
        progress.setStartedAt(now.minusSeconds(activeSeconds));
        progress.setLastAccessedAt(now);

        return progress;
    }
}