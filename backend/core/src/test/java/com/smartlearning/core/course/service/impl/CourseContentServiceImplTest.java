package com.smartlearning.core.course.service.impl;

import com.smartlearning.common.error.ApplicationException;
import com.smartlearning.common.error.CommonErrorCode;
import com.smartlearning.core.course.dto.request.CourseChapterCreateRequest;
import com.smartlearning.core.course.dto.request.CourseTopicCreateRequest;
import com.smartlearning.core.course.dto.response.CourseChapterResponse;
import com.smartlearning.core.course.dto.response.CourseTopicResponse;
import com.smartlearning.core.course.entity.CourseChapter;
import com.smartlearning.core.course.entity.CourseTopic;
import com.smartlearning.core.course.mapper.CourseChapterMapper;
import com.smartlearning.core.course.mapper.CourseTopicMapper;
import com.smartlearning.core.course.messaging.event.TopicKnowledgeIndexRequestedEvent;
import com.smartlearning.core.course.messaging.event.TopicKnowledgeOperation;
import com.smartlearning.core.course.repository.CourseChapterRepository;
import com.smartlearning.core.course.repository.CourseTopicRepository;
import com.smartlearning.core.course.sercurity.CourseAccessPolicy;
import com.smartlearning.core.course.utils.CourseUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.smartlearning.core.support.CoreTestData.CHAPTER_ID;
import static com.smartlearning.core.support.CoreTestData.COURSE_ID;
import static com.smartlearning.core.support.CoreTestData.OWNER_ID;
import static com.smartlearning.core.support.CoreTestData.STUDENT_ID;
import static com.smartlearning.core.support.CoreTestData.TOPIC_ID;
import static com.smartlearning.core.support.CoreTestData.chapter;
import static com.smartlearning.core.support.CoreTestData.chapterResponse;
import static com.smartlearning.core.support.CoreTestData.course;
import static com.smartlearning.core.support.CoreTestData.topic;
import static com.smartlearning.core.support.CoreTestData.topicResponse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CourseContentServiceImplTest {

    @Mock
    private CourseUtils courseUtils;
    @Mock
    private CourseAccessPolicy courseAccessPolicy;
    @Mock
    private CourseChapterRepository chapterRepository;
    @Mock
    private CourseTopicRepository topicRepository;
    @Mock
    private CourseChapterMapper chapterMapper;
    @Mock
    private CourseTopicMapper topicMapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @InjectMocks
    private CourseContentServiceImpl contentService;

    @Test
    void getChapters_allowsActiveMember() {
        CourseChapter chapter = chapter();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterRepository.findAllByCourseIdAndDeletedAtIsNullOrderByOrderIndexAsc(COURSE_ID))
                .thenReturn(List.of(chapter));
        when(chapterMapper.toResponse(chapter)).thenReturn(chapterResponse(chapter));

        List<CourseChapterResponse> result = contentService.getChapters(COURSE_ID, STUDENT_ID);

        assertThat(result).hasSize(1);
        verify(courseAccessPolicy).requireActiveMember(COURSE_ID, STUDENT_ID);
        verify(courseAccessPolicy, never()).requireOwner(COURSE_ID, STUDENT_ID);
    }

    @Test
    void getTopics_allowsActiveMember() {
        CourseChapter chapter = chapter();
        CourseTopic topic = topic(chapter);
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterRepository.findByIdAndCourseIdAndDeletedAtIsNull(CHAPTER_ID, COURSE_ID))
                .thenReturn(Optional.of(chapter));
        when(topicRepository.findAllByChapterIdAndDeletedAtIsNullOrderByOrderIndexAsc(CHAPTER_ID))
                .thenReturn(List.of(topic));
        when(topicMapper.toResponse(topic)).thenReturn(topicResponse(topic));

        List<CourseTopicResponse> result = contentService.getTopics(
                COURSE_ID,
                CHAPTER_ID,
                STUDENT_ID
        );

        assertThat(result).hasSize(1);
        verify(courseAccessPolicy).requireActiveMember(COURSE_ID, STUDENT_ID);
        verify(courseAccessPolicy, never()).requireOwner(COURSE_ID, STUDENT_ID);
    }

    @Test
    void createChapter_assignsNextOrderAndTrimsTitle() {
        CourseChapterCreateRequest request = new CourseChapterCreateRequest(
                "  Chương 1  ",
                "Mô tả",
                "Mục tiêu",
                null
        );
        CourseChapter mapped = new CourseChapter();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterMapper.toEntity(request)).thenReturn(mapped);
        when(chapterRepository.findMaxOrderIndex(COURSE_ID)).thenReturn(2);
        when(chapterRepository.save(mapped)).thenAnswer(invocation -> {
            mapped.setId(CHAPTER_ID);
            return mapped;
        });
        when(chapterMapper.toResponse(mapped)).thenAnswer(invocation -> chapterResponse(mapped));

        CourseChapterResponse result = contentService.createChapter(COURSE_ID, request, OWNER_ID);

        assertThat(result.orderIndex()).isEqualTo(3);
        assertThat(result.title()).isEqualTo("Chương 1");
        assertThat(mapped.getCourse().getId()).isEqualTo(COURSE_ID);
        verify(courseAccessPolicy).requireOwner(COURSE_ID, OWNER_ID);
    }

    @Test
    void createChapter_rejectsDuplicateOrder() {
        CourseChapterCreateRequest request = new CourseChapterCreateRequest(
                "Chương trùng",
                null,
                null,
                1
        );
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterMapper.toEntity(request)).thenReturn(new CourseChapter());
        when(chapterRepository.existsByCourseIdAndOrderIndex(COURSE_ID, 1)).thenReturn(true);

        assertThatThrownBy(() -> contentService.createChapter(COURSE_ID, request, OWNER_ID))
                .isInstanceOf(ApplicationException.class)
                .extracting(exception -> ((ApplicationException) exception).getErrorCode())
                .isEqualTo(CommonErrorCode.DATA_CONFLICT);

        verify(chapterRepository, never()).save(any());
    }

    @Test
    void createTopic_linksTopicToChapterAndAssignsNextOrder() {
        CourseChapter chapter = chapter();
        CourseTopicCreateRequest request = new CourseTopicCreateRequest(
                "  Chủ đề 1  ",
                "Mô tả",
                null,
                30,
                null
        );
        CourseTopic mapped = new CourseTopic();
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterRepository.findByIdAndCourseIdAndDeletedAtIsNull(CHAPTER_ID, COURSE_ID))
                .thenReturn(Optional.of(chapter));
        when(topicMapper.toEntity(request)).thenReturn(mapped);
        when(topicRepository.findMaxOrderIndex(CHAPTER_ID)).thenReturn(-1);
        when(topicRepository.save(mapped)).thenAnswer(invocation -> {
            mapped.setId(TOPIC_ID);
            return mapped;
        });
        when(topicMapper.toResponse(mapped)).thenAnswer(invocation -> topicResponse(mapped));

        CourseTopicResponse result = contentService.createTopic(
                COURSE_ID,
                CHAPTER_ID,
                request,
                OWNER_ID
        );

        assertThat(result.orderIndex()).isZero();
        assertThat(result.chapterId()).isEqualTo(CHAPTER_ID);
        assertThat(result.title()).isEqualTo("Chủ đề 1");
        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        TopicKnowledgeIndexRequestedEvent knowledgeEvent =
                (TopicKnowledgeIndexRequestedEvent) eventCaptor.getValue();
        assertThat(knowledgeEvent.courseId()).isEqualTo(COURSE_ID);
        assertThat(knowledgeEvent.chapterId()).isEqualTo(CHAPTER_ID);
        assertThat(knowledgeEvent.topicId()).isEqualTo(TOPIC_ID);
        assertThat(knowledgeEvent.operation()).isEqualTo(TopicKnowledgeOperation.UPSERT);
    }

    @Test
    void deleteChapter_cleansDocumentsAndPublishesTopicDeletionEvents() {
        CourseChapter chapter = chapter();
        CourseTopic first = topic(chapter);
        CourseTopic second = topic(chapter);
        second.setId(UUID.randomUUID());
        when(courseUtils.requireCourse(COURSE_ID)).thenReturn(course());
        when(chapterRepository.findByIdAndCourseIdAndDeletedAtIsNull(CHAPTER_ID, COURSE_ID))
                .thenReturn(Optional.of(chapter));
        when(topicRepository.findAllByChapterIdOrderByOrderIndexAsc(CHAPTER_ID))
                .thenReturn(List.of(first, second));

        contentService.deleteChapter(COURSE_ID, CHAPTER_ID, OWNER_ID);

        assertThat(chapter.getDeletedAt()).isNotNull();
        verify(topicRepository).deleteAll(List.of(first, second));
        verify(chapterRepository).delete(chapter);

        ArgumentCaptor<Object> events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(events.capture());
        assertThat(events.getAllValues())
                .allSatisfy(event -> assertThat(
                        ((TopicKnowledgeIndexRequestedEvent) event).operation()
                ).isEqualTo(TopicKnowledgeOperation.DELETE));
    }
}
